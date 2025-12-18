package me.osku.freeshiamy.settings

object SettingsKeys {
    const val KEY_KEYBOARD_HEIGHT_PERCENT = "keyboard_height_percent"
    const val MIN_KEYBOARD_HEIGHT_PERCENT = 90

    const val KEY_SWAP_DELETE_APOSTROPHE = "swap_delete_apostrophe"

    const val KEY_CANDIDATE_INLINE_LIMIT = "candidate_inline_limit"
    const val KEY_CANDIDATE_MORE_LIMIT = "candidate_more_limit"

    const val KEY_SHOW_SHORTEST_CODE_HINT = "show_shortest_code_hint"

    const val KEY_DISABLE_IME_IN_SENSITIVE_FIELDS = "disable_ime_in_sensitive_fields"
    const val KEY_SENSITIVE_FIELD_INCLUDE_NO_PERSONALIZED_LEARNING = "sensitive_field_include_no_personalized_learning"

    const val DEFAULT_KEYBOARD_HEIGHT_PERCENT = 100
    const val DEFAULT_SWAP_DELETE_APOSTROPHE = false
    const val DEFAULT_CANDIDATE_INLINE_LIMIT = 10
    const val DEFAULT_CANDIDATE_MORE_LIMIT = 200
    const val DEFAULT_SHOW_SHORTEST_CODE_HINT = true
    const val DEFAULT_DISABLE_IME_IN_SENSITIVE_FIELDS = true
    const val DEFAULT_SENSITIVE_FIELD_INCLUDE_NO_PERSONALIZED_LEARNING = false
}
