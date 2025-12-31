package me.osku.freeshiamy.settings

object SettingsKeys {
    const val KEY_KEYBOARD_HEIGHT_PERCENT = "keyboard_height_percent"
    const val MIN_KEYBOARD_HEIGHT_PERCENT = 90

    const val KEY_KEYBOARD_LAYOUT = "keyboard_layout"
    const val KEY_SHOW_NUMBER_ROW = "show_number_row"
    const val KEY_KEYBOARD_LEFT_SHIFT = "keyboard_left_shift"
    const val KEY_KEYBOARD_LABEL_TOP = "keyboard_label_top"
    const val KEY_ALWAYS_SHOW_IME = "always_show_ime"

    const val KEY_CANDIDATE_INLINE_LIMIT = "candidate_inline_limit"
    const val KEY_CANDIDATE_MORE_LIMIT = "candidate_more_limit"

    const val KEY_SHOW_SHORTEST_CODE_HINT = "show_shortest_code_hint"

    const val KEY_DISABLE_IME_IN_SENSITIVE_FIELDS = "disable_ime_in_sensitive_fields"
    const val KEY_SENSITIVE_FIELD_INCLUDE_NO_PERSONALIZED_LEARNING = "sensitive_field_include_no_personalized_learning"

    const val DEFAULT_KEYBOARD_HEIGHT_PERCENT = 100
    const val DEFAULT_KEYBOARD_LAYOUT = "standard"
    const val DEFAULT_SHOW_NUMBER_ROW = true
    const val DEFAULT_KEYBOARD_LEFT_SHIFT = true
    const val DEFAULT_KEYBOARD_LABEL_TOP = false
    const val DEFAULT_ALWAYS_SHOW_IME = true
    const val DEFAULT_CANDIDATE_INLINE_LIMIT = 10
    const val DEFAULT_CANDIDATE_MORE_LIMIT = 200
    const val DEFAULT_SHOW_SHORTEST_CODE_HINT = true
    const val DEFAULT_DISABLE_IME_IN_SENSITIVE_FIELDS = true
    const val DEFAULT_SENSITIVE_FIELD_INCLUDE_NO_PERSONALIZED_LEARNING = false
}
