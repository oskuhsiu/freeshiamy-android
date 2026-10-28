package me.osku.freeshiamy

import android.content.Context
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.util.Xml
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import me.osku.freeshiamy.catalog.CatalogCategory
import me.osku.freeshiamy.catalog.CatalogMode
import me.osku.freeshiamy.catalog.SymbolCatalog
import org.xmlpull.v1.XmlPullParser
import kotlin.math.roundToInt

/** A lazy catalog surface, separate from the letter keyboard and its composing state. */
internal class SymbolPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    interface Listener {
        fun onText(text: String)
        fun onKey(code: Int)
        fun onDeleteTouch(down: Boolean)
    }

    var listener: Listener? = null
    var mode: CatalogMode = CatalogMode.SYMBOLS
        private set
    private val catalog by lazy { SymbolCatalog(context) }
    private val savedPages = mutableMapOf<CatalogMode, Int>()
    private var categories = emptyList<CatalogCategory>()
    private var starts = emptyList<Int>()
    private var activeAdapter: CatalogAdapter? = null
    private var requestedHeightScale = 1f
    private var keyHaptics = true
    private var modeMenu: PopupMenu? = null
    private var revealSelection: Runnable? = null
    private val forwardingListener = object : Listener {
        override fun onText(text: String) { listener?.onText(text) }
        override fun onKey(code: Int) { listener?.onKey(code) }
        override fun onDeleteTouch(down: Boolean) { listener?.onDeleteTouch(down) }
    }
    private val modeButton = controlText().apply {
        setPadding(dp(6), 0, dp(6), 0)
        setOnClickListener { openModeMenu() }
    }
    private val categoryStrip = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val categoryScroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(categoryStrip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }
    private val pageCount = controlText().apply {
        textSize = 11f
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val previous = pageButton(R.string.catalog_previous_page, false) { movePage(-1) }
    private val next = pageButton(R.string.catalog_next_page, true) { movePage(1) }
    private val pager = ViewPager(context).apply {
        id = View.generateViewId()
        offscreenPageLimit = 1
        overScrollMode = OVER_SCROLL_NEVER
        addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                activeAdapter?.cancelGestures()
                savedPages[mode] = position
                updateSelection(position)
            }
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) activeAdapter?.cancelGestures()
            }
        })
    }
    private val footer: FreeShiamyKeyboardView
    private val footerKeyboard: FreeShiamyKeyboard

    init {
        orientation = VERTICAL
        setBackgroundColor(resources.getColor(R.color.keyboard_background, null))
        val controls = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(modeButton, LayoutParams(dp(68), LayoutParams.MATCH_PARENT))
            addView(categoryScroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(previous, LayoutParams(dp(48), LayoutParams.MATCH_PARENT))
            addView(pageCount, LayoutParams(dp(36), LayoutParams.MATCH_PARENT))
            addView(next, LayoutParams(dp(48), LayoutParams.MATCH_PARENT))
        }
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, dp(156)))
        // Keep the footer's KeyboardView appearance and native key behavior with a one-row layout.
        footer = resources.getXml(R.xml.catalog_footer).use { parser ->
            while (parser.eventType != XmlPullParser.START_TAG && parser.eventType != XmlPullParser.END_DOCUMENT) parser.next()
            FreeShiamyKeyboardView(context, Xml.asAttributeSet(parser))
        }
        footerKeyboard = FreeShiamyKeyboard(context, R.xml.catalog_footer)
        footer.keyboard = footerKeyboard
        footer.setPadding(0, 0, 0, 0)
        footer.setBackgroundColor(resources.getColor(R.color.keyboard_background, null))
        footer.setOnKeyboardActionListener(object : KeyboardView.OnKeyboardActionListener {
            override fun onPress(primaryCode: Int) {
                if (primaryCode != 0) footer.performKeyHapticFeedback()
            }
            override fun onRelease(primaryCode: Int) = Unit
            override fun onKey(primaryCode: Int, keyCodes: IntArray?) { listener?.onKey(primaryCode) }
            override fun onText(text: CharSequence?) { text?.let { listener?.onText(it.toString()) } }
            override fun swipeLeft() = Unit
            override fun swipeRight() = Unit
            override fun swipeDown() = Unit
            override fun swipeUp() = Unit
        })
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun show(mode: CatalogMode) {
        if (activeAdapter != null && this.mode == mode) return
        releasePages()
        this.mode = mode
        categories = catalog.categories(mode)
        var start = 0
        starts = categories.map { category -> start.also { start += category.pageCount } }
        modeButton.text = modeLabel(mode)
        modeButton.contentDescription = context.getString(R.string.catalog_choose_mode, modeLabel(mode))
        categoryStrip.removeAllViews()
        categories.forEachIndexed { index, category ->
            categoryStrip.addView(controlText().apply {
                text = category.label
                setPadding(dp(12), 0, dp(12), 0)
                setOnClickListener {
                    activeAdapter?.cancelGestures()
                    if (categoryIndex(pager.currentItem) != index) pager.setCurrentItem(starts[index], false)
                }
            }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
        val adapter = CatalogAdapter(start)
        activeAdapter = adapter
        pager.adapter = adapter
        val page = (savedPages[mode] ?: 0).coerceIn(0, adapter.count - 1)
        pager.setCurrentItem(page, false)
        updateSelection(page)
    }

    /** Keep only metadata and one integer per mode while the picker is not used. */
    fun releasePages() {
        if (activeAdapter != null) savedPages[mode] = pager.currentItem
        modeMenu?.dismiss()
        modeMenu = null
        revealSelection?.let { categoryScroll.removeCallbacks(it) }
        revealSelection = null
        footer.closing()
        activeAdapter?.cancelGestures()
        pager.adapter = null
        activeAdapter = null
    }

    fun setHeightScale(scale: Float) {
        requestedHeightScale = scale.coerceIn(0.9f, 2.2f)
        footer.setHeightScale(requestedHeightScale)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) Float.MAX_VALUE
            else (MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom - dp(48)).coerceAtLeast(4).toFloat()
        val bodyScale = minOf(requestedHeightScale, available / dp(212))
        pager.layoutParams.height = (dp(156) * bodyScale).roundToInt().coerceAtLeast(3)
        footer.layoutParams.height = (dp(56) * bodyScale).roundToInt().coerceAtLeast(1)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setImeOptions(options: Int) {
        footerKeyboard.setImeOptions(resources, options)
        footer.invalidateAllKeys()
    }

    fun setOutputModeLabel(simplified: Boolean) {
        footer.setOutputModeLabel(simplified)
    }

    fun setSpaceSwipeEnabled(enabled: Boolean) {
        footer.setSpaceSwipeEnabled(enabled)
    }

    override fun setHapticFeedbackEnabled(enabled: Boolean) {
        super.setHapticFeedbackEnabled(enabled)
        keyHaptics = enabled
        footer.setKeyHapticFeedbackEnabled(enabled)
    }

    fun handleBack(): Boolean {
        if (modeMenu != null) {
            modeMenu?.dismiss()
            modeMenu = null
            return true
        }
        if (footer.handleBack()) return true
        return activeAdapter?.pages?.values?.any { it.handleBack() } == true
    }

    override fun onDetachedFromWindow() {
        releasePages()
        super.onDetachedFromWindow()
    }

    private fun openModeMenu() {
        activeAdapter?.cancelGestures()
        modeMenu?.dismiss()
        val menu = PopupMenu(context, modeButton)
        CatalogMode.values().forEach { item ->
            menu.menu.add(0, item.ordinal, item.ordinal, modeLabel(item)).apply {
                isCheckable = true
                isChecked = item == mode
            }
        }
        menu.setOnMenuItemClickListener { item ->
            show(CatalogMode.values()[item.itemId])
            true
        }
        menu.setOnDismissListener { if (modeMenu === menu) modeMenu = null }
        modeMenu = menu
        menu.show()
    }

    private fun movePage(delta: Int) {
        val adapter = activeAdapter ?: return
        val target = (pager.currentItem + delta).coerceIn(0, adapter.count - 1)
        adapter.cancelGestures()
        pager.setCurrentItem(target, true)
    }

    private fun updateSelection(position: Int) {
        if (categories.isEmpty()) return
        val categoryIndex = categoryIndex(position)
        val category = categories[categoryIndex]
        val localPage = position - starts[categoryIndex] + 1
        pageCount.text = "$localPage/${category.pageCount}"
        pager.contentDescription = context.getString(R.string.catalog_page_description,
            category.label, localPage, category.pageCount)
        previous.isEnabled = position > 0
        next.isEnabled = position < (activeAdapter?.count ?: 0) - 1
        previous.alpha = if (previous.isEnabled) 1f else 0.35f
        next.alpha = if (next.isEnabled) 1f else 0.35f
        for (index in categories.indices) {
            val child = categoryStrip.getChildAt(index) as TextView
            child.isSelected = index == categoryIndex
            child.setTextColor(resources.getColor(if (child.isSelected) R.color.candidate_recommended else R.color.candidate_other, null))
            child.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,
                if (child.isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        val selected = categoryStrip.getChildAt(categoryIndex)
        revealSelection?.let { categoryScroll.removeCallbacks(it) }
        revealSelection = Runnable {
            if (selected.parent === categoryStrip) categoryScroll.smoothScrollTo(
                (selected.left - (categoryScroll.width - selected.width) / 2).coerceAtLeast(0), 0)
        }.also { categoryScroll.post(it) }
    }

    private fun categoryIndex(page: Int): Int = starts.indexOfLast { it <= page }.coerceAtLeast(0)

    private fun modeLabel(value: CatalogMode): String = context.getString(when (value) {
        CatalogMode.SYMBOLS -> R.string.catalog_symbols
        CatalogMode.EMOJI -> R.string.catalog_emoji
        CatalogMode.EMOTICONS -> R.string.catalog_emoticons
    })

    private fun controlText() = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 14f
        isFocusable = true
        setTextColor(resources.getColor(R.color.candidate_other, null))
        background = context.getDrawable(R.drawable.keyboard_key_background)
    }

    private fun pageButton(description: Int, forward: Boolean, action: () -> Unit) = ImageButton(context).apply {
        setImageResource(R.drawable.ic_keyboard_back)
        rotation = if (forward) 180f else 0f
        contentDescription = context.getString(description)
        setPadding(dp(8), dp(12), dp(8), dp(12))
        background = context.getDrawable(R.drawable.keyboard_key_background)
        setOnClickListener { action() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    private inner class CatalogAdapter(private val total: Int) : PagerAdapter() {
        val pages = mutableMapOf<Int, SymbolGridPageView>()
        override fun getCount() = total
        override fun isViewFromObject(view: View, value: Any) = view === value
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val index = categoryIndex(position)
            val page = SymbolGridPageView(context, mode,
                catalog.loadPage(categories[index], position - starts[index]), forwardingListener) { keyHaptics }
            pages[position] = page
            container.addView(page)
            return page
        }
        override fun destroyItem(container: ViewGroup, position: Int, value: Any) {
            val page = value as SymbolGridPageView
            page.release()
            container.removeView(page)
            pages.remove(position)
        }
        fun cancelGestures() { pages.values.toList().forEach { it.release() } }
    }
}
