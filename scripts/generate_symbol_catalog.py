#!/usr/bin/env python3
"""Generate tiny UTF-8 catalog pages from the checked-in research archive.

Run without arguments to regenerate, or with --check to verify every packaged
byte and round-trip all positions and long-press variants against the archive.
No network access or third-party Python packages are needed. The index columns
are mode, minimum API, ID, label, page count, item count, asset path prefix.
Each page row is the main text followed by its ordered variants (TSV). Literal
archive strings retain Android XML resource escapes; these are decoded once.
Unicode sequences and all whitespace remain unchanged.
"""

import argparse
from dataclasses import dataclass
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / "doc/google-fatty-zhuyin-catalog.json"
OUTPUT = ROOT / "app/src/main/assets/catalog"
MAX_PAGE_BYTES = 16 * 1024
CAPACITIES = {"SYMBOLS": 28, "EMOJI": 20, "EMOTICONS": 11}


@dataclass
class Category:
    mode: str
    min_api: int
    id: str
    label: str
    items: list

    @property
    def pages(self):
        capacity = CAPACITIES[self.mode]
        return [self.items[i:i + capacity] for i in range(0, len(self.items), capacity)]

    @property
    def asset_path(self):
        return f"catalog/{self.mode.lower()}/{self.min_api}/{self.id}"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def decode_literal(text):
    """Decode AAPT XML-attribute escapes, without changing quotes or spaces.

    Unlike values/string resources, these custom XML attributes retain literal
    quotes. AAPT removes a backslash before ordinary characters (including ^),
    decodes escaped backslashes once, and supports n/t/u escapes. Do not use
    Python's unicode_escape: it corrupts literal non-ASCII text.
    """
    result = []
    offset = 0
    while offset < len(text):
        char = text[offset]
        offset += 1
        if char != "\\":
            result.append(char)
            continue
        if offset == len(text):
            break  # A bare trailing escape is consumed by AAPT.
        escaped = text[offset]
        offset += 1
        if escaped == "u":
            digits = text[offset:offset + 4]
            require(len(digits) == 4 and all(c in "0123456789abcdefABCDEF" for c in digits),
                    "Invalid Android Unicode escape")
            result.append(chr(int(digits, 16)))
            offset += 4
        else:
            result.append({"n": "\n", "t": "\t"}.get(escaped, escaped))
    return "".join(result)


def entry_text(entry):
    return entry["text"] if entry["codepoints"] else decode_literal(entry["text"])


def fields(item):
    values = [entry_text(item)] + [entry_text(variant) for variant in item["long_press_variants"]]
    for value in values:
        require(isinstance(value, str) and value != "", "Empty or non-string catalog text")
        require(not any(char in value for char in "\t\r\n"), "TSV delimiter in catalog text")
    # Compare the recorded Unicode sequences too; no normalization is permitted.
    for entry in [item] + item["long_press_variants"]:
        if entry["codepoints"]:
            sequence = "".join(chr(int(cp[2:], 16)) for cp in entry["codepoints"])
            require(sequence == entry["text"], "Archive codepoint/text mismatch")
    return values


def source_categories(archive):
    datasets = {(data["path"], data["qualifier"]): data for data in archive["datasets"]}
    routes = {route["path"]: route for route in archive["route_category_summaries"]}
    streams = {stream["path"]: stream for stream in archive["expanded_include_streams"]}
    categories = []
    stats = {"associations": [], "source_extra": 0, "legacy_extra": 0}

    # The TW expanded stream includes the number/basic-symbol includes in the
    # first category; taking only its directly defined dataset would lose 20.
    symbol_route = "src/res/xml/softkeys_input_symbol_sub_categories_tw.xml"
    stream = streams[symbol_route]
    require(not stream["errors"], "Unresolved TW symbol includes")
    offset = 0
    symbol_labels = [
        ("favorite", "常用"), ("brackets", "括號"), ("arrows", "箭頭"),
        ("math", "數學"), ("numbers", "數字"), ("shapes", "圖形"),
    ]
    segments = routes[symbol_route]["segments"]
    require(len(segments) == len(symbol_labels), "Unexpected symbol category count")
    for segment, (slug, label) in zip(segments, symbol_labels):
        require(not segment["include_errors"], "Unresolved symbol category")
        end = offset + segment["static_items"]
        categories.append(Category("SYMBOLS", 23, f"symbols_{slug}", label, stream["items"][offset:end]))
        offset = end
    require(offset == len(stream["items"]) == 532, "Expected 532 TW symbol positions")

    # Preserve the TW layout, while keeping phone-base characters reachable.
    # Restore a base long-press association only when the TW main has none.
    base_stream = streams["src/res/xml/softkeys_input_symbol_sub_categories.xml"]
    require(not base_stream["errors"], "Unresolved base symbol includes")
    base_with_variants = {}
    for item in base_stream["items"]:
        if item["long_press_variants"]:
            base_with_variants.setdefault(entry_text(item), item["long_press_variants"])
    for category in categories:
        merged = []
        for item in category.items:
            variants = base_with_variants.get(entry_text(item), [])
            if not item["long_press_variants"] and variants:
                item = dict(item, long_press_variants=variants)
                stats["associations"].append((entry_text(item), [entry_text(v) for v in variants]))
            merged.append(item)
        category.items = merged

    reachable = {text for cat in categories for item in cat.items for text in fields(item)}
    extra = []
    for item in base_stream["items"]:
        # Main precedes its variants in the source stream. A new main retains
        # its own variants; missing variants of existing mains stand alone.
        for entry in [item] + item["long_press_variants"]:
            if entry_text(entry) in reachable:
                continue
            added = entry if entry is item else dict(entry, long_press_variants=[])
            extra.append(added)
            reachable.update(fields(added))
    stats["source_extra"] = len(extra)
    if "´" not in reachable:
        extra.append({"text": "´", "codepoints": ["U+00B4"], "long_press_variants": []})
        stats["legacy_extra"] = 1
    categories.append(Category("SYMBOLS", 23, "symbols_extra", "補充", extra))
    require((stats["source_extra"], stats["legacy_extra"], len(stats["associations"])) == (9, 1, 9),
            "Unexpected phone-base symbol union")
    require(sum(len(cat.items) for cat in categories) == 542, "Expected 542 union symbol positions")
    require(sum(len(item["long_press_variants"]) for cat in categories for item in cat.items) == 87,
            "Expected 87 union symbol long-press positions")

    emoji_labels = [
        ("smiley_people", "表情與人物"), ("animals_nature", "動物與自然"),
        ("food_drink", "食物與飲料"), ("travel_places", "旅遊與地點"),
        ("activity", "活動"), ("objects", "物品"),
        ("symbols", "標誌"), ("flags", "旗幟"),
    ]
    for min_api, qualifier, expected_items, expected_variants in [
        (23, "base", 1357, 504), (25, "xml-v25", 1404, 880),
    ]:
        emoji_categories = []
        for number, (slug, label) in enumerate(emoji_labels, start=2):
            route = routes[f"src/res/xml/softkeys_input_emoji_sub_category_{number}.xml"]
            require(len(route["segments"]) == 1, "Unexpected Emoji category route")
            segment = route["segments"][0]
            selected = segment if min_api == 23 else segment["qualifier_override"]
            require(not selected["include_errors"], "Unresolved Emoji category")
            dataset = datasets[(selected["target"], qualifier)]
            require(len(dataset["items"]) == selected["static_items"], "Emoji route count mismatch")
            emoji_categories.append(Category("EMOJI", min_api, f"emoji_{slug}", label, dataset["items"]))
        require(sum(len(cat.items) for cat in emoji_categories) == expected_items, "Emoji position count mismatch")
        require(sum(len(item["long_press_variants"]) for cat in emoji_categories for item in cat.items)
                == expected_variants, "Emoji long-press count mismatch")
        categories.extend(emoji_categories)

    emoticon_labels = [
        ("smile", "微笑"), ("sweat", "冒汗"), ("surprise", "驚訝"),
        ("sadness", "難過"), ("displeasure", "不滿"),
    ]
    segments = routes["src/res/xml/softkeys_input_emoticon.xml"]["segments"]
    require(len(segments) == len(emoticon_labels), "Unexpected emoticon category count")
    for segment, (slug, label) in zip(segments, emoticon_labels):
        require(not segment["include_errors"], "Unresolved emoticon category")
        dataset = datasets[(segment["target"], "base")]
        require(len(dataset["items"]) == segment["static_items"], "Emoticon route count mismatch")
        categories.append(Category("EMOTICONS", 23, f"emoticons_{slug}", label, dataset["items"]))
    require(sum(len(cat.items) for cat in categories if cat.mode == "EMOTICONS") == 90,
            "Expected 90 emoticon positions")
    return categories, stats


def generated_files(categories):
    files = {}
    index = []
    for category in categories:
        prefix = category.asset_path.removeprefix("catalog/")
        index.append("\t".join(map(str, [category.mode, category.min_api, category.id,
                                      category.label, len(category.pages), len(category.items),
                                      category.asset_path])))
        for number, page in enumerate(category.pages):
            payload = "".join("\t".join(fields(item)) + "\n" for item in page).encode("utf-8")
            require(len(payload) <= MAX_PAGE_BYTES, f"Page exceeds 16 KiB: {prefix}/{number}")
            files[f"{prefix}/{number}.tsv"] = payload
    files["index.tsv"] = ("\n".join(index) + "\n").encode("utf-8")
    require(len(files["index.tsv"]) <= MAX_PAGE_BYTES, "Index exceeds 16 KiB")
    return files


def check(categories, files, archive):
    # These custom XML-attribute fixtures were verified with AAPT2 compile/link
    # and dump xmltree, rather than applying values/string quote/space rules.
    for raw, expected in [
        (r"\@", "@"), (r"\#", "#"), (r"\?", "?"),
        (r"\\", "\\"), (r"\^o^/", "^o^/"),
        ("'", "'"), ('"', '"'), (r"\'", "'"), (r'\"', '"'),
        ("( ˘•ω•˘ )", "( ˘•ω•˘ )"), (r"\\^o^/", "\\^o^/"),
        ("  ( ^‿^ )  ", "  ( ^‿^ )  "),
        (r"\n", "\n"), (r"\t", "\t"), (r"\r", "r"), (r"\u263A", "☺"),
        (r"\\n", r"\n"), ("\\", ""),
    ]:
        require(decode_literal(raw) == expected, f"Android escape fixture failed: {raw!r}")
    require(entry_text({"text": r"\@", "codepoints": ["U+005C", "U+0040"]}) == r"\@",
            "Unicode sequence must bypass literal escape decoding")
    symbol_categories = [cat for cat in categories if cat.mode == "SYMBOLS"]
    repertoire = {text for cat in symbol_categories for item in cat.items for text in fields(item)}
    for stream in archive["expanded_include_streams"]:
        if stream["path"] in ["src/res/xml/softkeys_input_symbol_sub_categories.xml",
                              "src/res/xml/softkeys_input_symbol_sub_categories_tw.xml"]:
            source_repertoire = {text for item in stream["items"] for text in fields(item)}
            require(source_repertoire <= repertoire, "Missing base/TW phone symbol text")
    extra_texts = [entry_text(item) for item in symbol_categories[-1].items]
    original_repertoire = {text for cat in symbol_categories[:-1] for item in cat.items for text in fields(item)}
    require(len(extra_texts) == len(set(extra_texts)), "Duplicate supplemental main text")
    require(not (set(extra_texts) & original_repertoire), "Supplemental main already available in TW")
    # Main grid keys from the former symbols/symbols_shift XML files, plus
    # their comma/period footer keys. Space, Enter and control keys are separate.
    legacy = "1234567890@#$%&*-=()!\"':;/?" + "~±×÷•°`´{}©£€^®¥_+[]¡<>¢|\\¿" + ",."
    require(len(legacy) == 56, "Legacy symbol fixture count mismatch")
    require(set(legacy) <= repertoire | {",", "."}, "Missing legacy symbol key")
    actual_names = {str(path.relative_to(OUTPUT)) for path in OUTPUT.rglob("*") if path.is_file()}
    require(actual_names == set(files), f"Generated file set differs: {actual_names ^ set(files)}")
    for name, expected in files.items():
        require((OUTPUT / name).read_bytes() == expected, f"Stale asset: {name}")
    index_rows = (OUTPUT / "index.tsv").read_text(encoding="utf-8").splitlines()
    require(len(index_rows) == len(categories), "Index category count mismatch")
    for category, row in zip(categories, index_rows):
        mode, minimum_api, identity, label, page_count, item_count, asset_path = row.split("\t")
        require((mode, int(minimum_api), identity, label, int(page_count), int(item_count), asset_path)
                == (category.mode, category.min_api, category.id, category.label,
                    len(category.pages), len(category.items), category.asset_path), "Index metadata mismatch")
        decoded = []
        for page_number, expected_page in enumerate(category.pages):
            payload = (OUTPUT.parent / asset_path / f"{page_number}.tsv").read_bytes()
            require(len(payload) <= MAX_PAGE_BYTES, "Packaged page exceeds 16 KiB")
            require(payload.endswith(b"\n"), "Page must end in LF")
            page = [line.split("\t") for line in payload.decode("utf-8")[:-1].split("\n")]
            require(page == [fields(item) for item in expected_page], "Page round-trip mismatch")
            require(0 < len(page) <= CAPACITIES[mode], "Page capacity mismatch")
            decoded.extend(page)
        require(decoded == [fields(item) for item in category.items], "Category order/variant mismatch")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify existing generated assets without writing")
    args = parser.parse_args()
    archive = json.loads(ARCHIVE.read_text(encoding="utf-8"))
    categories, stats = source_categories(archive)
    files = generated_files(categories)
    if not args.check:
        OUTPUT.mkdir(parents=True, exist_ok=True)
        # Remove only obsolete generated TSVs, preserving any other local work.
        for path in OUTPUT.rglob("*.tsv"):
            if str(path.relative_to(OUTPUT)) not in files:
                path.unlink()
        for name, payload in files.items():
            path = OUTPUT / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(payload)
    check(categories, files, archive)
    pages = [payload for name, payload in files.items() if name != "index.tsv"]
    print(f"Verified {len(categories)} category records, {len(pages)} pages, "
          f"{sum(map(len, files.values()))} bytes; largest page {max(map(len, pages))} bytes; "
          f"index {len(files['index.tsv'])} bytes.")
    for mode, min_api in [("SYMBOLS", 23), ("EMOJI", 23), ("EMOJI", 25), ("EMOTICONS", 23)]:
        selected = [cat for cat in categories if (cat.mode, cat.min_api) == (mode, min_api)]
        print(f"{mode} API {min_api}: {len(selected)} categories, "
              f"{sum(len(cat.items) for cat in selected)} positions, "
              f"{sum(len(item['long_press_variants']) for cat in selected for item in cat.items)} variants, "
              f"{sum(len(cat.pages) for cat in selected)} pages")
    transformed_items = sum(entry_text(item) != item["text"] for cat in categories for item in cat.items)
    transformed_variants = sum(entry_text(variant) != variant["text"] for cat in categories
                               for item in cat.items for variant in item["long_press_variants"])
    print(f"Decoded Android escapes in {transformed_items} item positions and "
          f"{transformed_variants} long-press variant positions.")
    print(f"Supplement: {stats['source_extra']} source-only main positions + "
          f"{stats['legacy_extra']} legacy accent position; all 56 old symbols reachable "
          "(comma/period available in footer).")
    print(f"Restored {len(stats['associations'])} base long-press associations: {stats['associations']!r}")


if __name__ == "__main__":
    main()
