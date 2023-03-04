package com.codersguidebook.recyclerviewfastscrollerwithvaluelabel

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A custom View that displays a scrollbar for RecyclerView widgets. The scrollbar features a draggable thumb
 * and value label.
 *
 *  FOR LIBRARY RELEASE:
 *   - Will need to create a test app that uses the library and confirm it works as a standalone library
 *   - The RecyclerView adapter must implement RecyclerView.Adapter and RecyclerViewScrollbar.ValueLabelListener
 *   - Demonstrate that you can override the OnScrollListener open class as usual
 *   - In the library package maybe the OnScrollListener can have its own file
 *   - The ScrollBar is only compatible with androidx.recyclerview.widget.RecyclerView
 *   - The RecyclerView must be assigned to the recyclerView variable or else scroll actions will not propagate
 *      Do this using code like binding.scrollBar.recyclerView = binding.recyclerView
 *
 *  BENEFITS OF THE LIBRARY:
 *   - The scrollbar thumb always has a minimum height (unlike the default fast scroll thumb, which
 *   can become too small when the RecyclerView has lots of content. This is a known issue that has been
 *   unresolved for years https://issuetracker.google.com/issues/64729576)
 *
 * @constructor Construct an instance of the scrollbar by supplying the context and attribute set required
 * for the layout editor to manage the View.
 *
 * @param context The context in which the scrollbar is being loaded (Application context is sufficient).
 * @param attrs Values for the scrollbar's customisable attributes.
 */
class RecyclerViewScrollbar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        const val DEFAULT_VALUE_LABEL_WIDTH = 200f
        const val DEFAULT_THUMB_AND_TRACK_WIDTH = 25f
    }

    var recyclerView: RecyclerView? = null

    private var recyclerViewContentHeight: Int? = null
    private var recyclerViewScrollPosition = 0

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var trackRect = Rect()

    private val thumbAndTrackWidth: Int
    private val thumbOffColour: Int
    private val thumbOnColour: Int
    private val thumbMinHeight: Int
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var thumbRect = Rect()
    private var thumbSelected = false

    private val valueLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var valueLabelText: String? = null
    private var valueLabelWidth: Float

    private val textBounds = Rect()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val animationDuration = context.resources
        .getInteger(android.R.integer.config_mediumAnimTime).toLong()

    var lastActionNanoTime = System.nanoTime()

    private var fadeOutScrollbarRunnable = object : Runnable {
        override fun run() {
            val timeElapsed = System.nanoTime() - lastActionNanoTime
            try {
                // If the scrollbar thumb is not selected and one second has passed then hide the thumb
                if (!thumbSelected && timeElapsed >= 1000000000) {
                    animate()
                        .alpha(0f)
                        .setDuration(animationDuration)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                visibility = INVISIBLE
                            }
                        })
                }
            } finally {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.RecyclerViewScrollbar, 0, 0).apply {

            try {
                thumbAndTrackWidth = getDimension(
                    R.styleable.RecyclerViewScrollbar_thumbAndTrackWidth,
                    DEFAULT_THUMB_AND_TRACK_WIDTH
                ).roundToInt()
                val defaultTrackColour =
                    MaterialColors.getColor(context, R.attr.colorOnSurface, Color.GRAY)
                // 30% Alpha
                val defaultTrackColour30 =
                    MaterialColors.compositeARGBWithAlpha(defaultTrackColour, 77)
                trackPaint.apply {
                    color = getInt(R.styleable.RecyclerViewScrollbar_trackColor, defaultTrackColour30)
                }

                val defaultThumbOffColour =
                    MaterialColors.getColor(context, R.attr.colorOnSurface, Color.LTGRAY)
                val defaultThumbOffColour84 =
                    MaterialColors.compositeARGBWithAlpha(defaultThumbOffColour, 214)
                thumbOffColour = getInt(R.styleable.RecyclerViewScrollbar_thumbOffColor, defaultThumbOffColour84)
                thumbPaint.apply { color = thumbOffColour }

                thumbMinHeight = getDimension(
                    R.styleable.RecyclerViewScrollbar_thumbMinHeight,
                    thumbAndTrackWidth * 4f).roundToInt()
                val defaultThumbOnColour =
                    MaterialColors.getColor(context, R.attr.colorSecondary, Color.CYAN)
                thumbOnColour = getInt(R.styleable.RecyclerViewScrollbar_thumbOnColor, defaultThumbOnColour)

                valueLabelWidth = getDimension(
                    R.styleable.RecyclerViewScrollbar_valueLabelWidth,
                    DEFAULT_VALUE_LABEL_WIDTH
                )
                valueLabelPaint.apply {
                    color = getInt(R.styleable.RecyclerViewScrollbar_valueLabelBackgroundColor, defaultThumbOnColour)
                }

                val defaultTextColour =
                    MaterialColors.getColor(context, R.attr.textFillColor, Color.BLACK)
                textPaint.apply {
                    color = getInt(R.styleable.RecyclerViewScrollbar_valueLabelTextColor, defaultTextColour)
                    style = Paint.Style.FILL
                    textAlign = Paint.Align.CENTER
                    textSize = getDimension(
                        R.styleable.RecyclerViewScrollbar_valueLabelTextSize,
                        valueLabelWidth / 2)
                }
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        visibility = INVISIBLE
        fadeOutScrollbarRunnable.run()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(fadeOutScrollbarRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // If the RecyclerView is not larger than the View, then no need to display the scrollbar
        if (measuredHeight >= (recyclerViewContentHeight ?: measuredHeight)) return

        canvas.apply {
            // Draw the track and thumb
            val savedState = save()
            translate((width - thumbAndTrackWidth).toFloat(), 0f)
            drawRect(trackRect, trackPaint)
            drawRect(thumbRect, thumbPaint)
            restoreToCount(savedState)

            if (thumbSelected && valueLabelText != null) {
                // Position the canvas so that the value label is drawn next to the center of the scrollbar
                // thumb, except when doing so would cause the value label to fall outside the View.
                val scrollbarThumbCenterYCoordinate = (thumbRect.height().toFloat() / 2) + thumbRect.top
                val yStartToUse = if (valueLabelWidth > scrollbarThumbCenterYCoordinate) 0f
                else scrollbarThumbCenterYCoordinate - valueLabelWidth

                translate(0f, yStartToUse)
                drawPath(getValueLabelPath(), valueLabelPaint)

                // Draw the appropriate value text for the position in the RecyclerView
                valueLabelText?.let { text ->
                    // Need to offset the text so it is visible while scrolling, but not so much that it
                    // falls outside the value label
                    val proposedXOffset = (valueLabelWidth / 2) - (thumbAndTrackWidth * 3)
                    val xOffsetToUse = max(proposedXOffset, (valueLabelWidth / 4))
                    val yOffsetToUse = (valueLabelWidth / 2) - (textBounds.top / 2)
                    drawText(text, xOffsetToUse, yOffsetToUse, textPaint)
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != 0 && h != 0) {
            trackRect.set(thumbAndTrackWidth, 0, 0, h)
            thumbRect.set(thumbAndTrackWidth, 0, 0, getThumbHeight())
        }
    }

    override fun getLayoutParams(): ViewGroup.LayoutParams {
        return super.getLayoutParams().apply {
            width = thumbAndTrackWidth + valueLabelWidth.roundToInt()
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) alpha = 1f
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val y = event?.y ?: 0f

        when (event?.action) {
            // Action down = 0; Action move = 2;
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                visibility = VISIBLE
                recyclerViewContentHeight?.let { height ->
                    val scrollProportion = y / measuredHeight
                    val newScrollPosition = scrollProportion * height
                    scrollToRecyclerViewPosition(newScrollPosition.toInt())
                }
                thumbSelected = true
                thumbPaint.color = thumbOnColour
                if (event.action == MotionEvent.ACTION_DOWN) invalidate()
                return true
            }
            // Action cancel = 3; Action up = 1;
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                thumbSelected = false
                thumbPaint.color = thumbOffColour
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * A method called when the scrollbar thumb is being dragged. Notifies
     * the associated RecyclerView (if set) with the new scroll position.
     *
     * @param position The position that the user has scrolled to.
     */
    private fun scrollToRecyclerViewPosition(position: Int) {
        recyclerView?.apply {
            val maximumScrollPosition = this.computeVerticalScrollRange()
            val scrollToProportion = if (position > maximumScrollPosition) 1f
            else position.toFloat() / maximumScrollPosition
            val scrollToPosition = scrollToProportion * (adapter?.itemCount ?: return)

            this.scrollToPosition(scrollToPosition.roundToInt())
        }
    }

    /** Move the thumb along the track to reflect the RecyclerView scroll position */
    private fun updateScrollPosition() {
        recyclerViewContentHeight?.let { height ->
            visibility = VISIBLE

            // The scroll proportion will be between 0 (top) and 1 (bottom)
            val scrollProportion = recyclerViewScrollPosition.toFloat() / height
            // The scroll position will be the height of the View * the scroll proportion
            val scrollPosition = measuredHeight * scrollProportion

            // Update the y coordinate of the thumb to reflect the scroll position
            // The y coordinate should not cause the thumb to go off the screen,
            // and so maximum y coordinate values are provided as a fallback.
            val maximumTopValue = measuredHeight - getThumbHeight()
            val topValueToUse = min(scrollPosition.toInt(), maximumTopValue)

            val proposedBottomValue = scrollPosition + getThumbHeight()
            val bottomValueToUse = min(proposedBottomValue.toInt(), measuredHeight)

            thumbRect.set(thumbAndTrackWidth, topValueToUse, 0, bottomValueToUse)

            invalidate()
            lastActionNanoTime = System.nanoTime()
        }
    }

    /**
     * Calculate the scrollbar value label's Path coordinates to reflect the updated scroll position.
     *
     * @return A Path object that draws the value label.
     */
    private fun getValueLabelPath(): Path {
        val valueLabelCornerOffset = valueLabelWidth / 4
        val valueLabelCornerMidway = valueLabelCornerOffset / 5

        return Path().apply {
            moveTo(valueLabelWidth, valueLabelWidth)
            lineTo(valueLabelWidth, valueLabelCornerOffset)

            // Draw the top right corner
            val topRightX1 = valueLabelWidth - valueLabelCornerMidway
            val topRightX2 = valueLabelWidth - valueLabelCornerOffset
            quadTo(topRightX1, valueLabelCornerMidway, topRightX2, 0f)

            // Draw the top left corner
            lineTo(valueLabelCornerOffset, 0f)
            quadTo(valueLabelCornerMidway, valueLabelCornerMidway, 0f, valueLabelCornerOffset)

            // Draw the bottom left corner
            lineTo(0f, valueLabelWidth - valueLabelCornerOffset)
            val bottomLeftY1 = valueLabelWidth - valueLabelCornerMidway
            quadTo(valueLabelCornerMidway, bottomLeftY1, valueLabelCornerOffset, valueLabelWidth)
        }
    }

    /**
     * Determine the appropriate height of the scrollbar thumb.
     *
     * @return An integer representing the height to use for the thumb. The height
     * will always be equal to or greater than $minimumThumbHeight
     */
    private fun getThumbHeight(): Int {
        val viewHeightProportionOfContentHeight = measuredHeight.toFloat() /
                (recyclerViewContentHeight ?: measuredHeight)
        val thumbHeight = measuredHeight * viewHeightProportionOfContentHeight
        return when {
            thumbHeight > measuredHeight -> measuredHeight
            thumbHeight > thumbMinHeight -> thumbHeight.roundToInt()
            else -> thumbMinHeight
        }
    }

    /**
     * Set the height of the RecyclerView's contents.
     *
     * @param height The cumulative total height of the RecyclerView's contents.
     */
    fun notifyRecyclerViewContentHeightChanged(height: Int) {
        if (height != recyclerViewContentHeight && height >= 0) {
            recyclerViewContentHeight = height
        }
    }

    /**
     * Set the RecyclerView scroll position and move the scrollbar thumb accordingly.
     *
     * @param position The scroll position.
     */
    fun notifyRecyclerViewScrollPositionChanged(position: Int) {
        recyclerViewScrollPosition = position
        updateScrollPosition()
    }

    /**
     * Set the text to display in the value label for a given scroll position.
     * Often, the text will be a single character.
     *
     * @param text A String containing the text to display in the value label at
     * a given scroll position.
     */
    fun setValueLabelText(text: String?) {
        valueLabelText = text
        // Transmit the Rect bounds of the value label text to the textBounds variable.
        textPaint.getTextBounds(text, 0, (text?.length ?: 0), textBounds)
    }

    interface ValueLabelListener {
        /**
         * Retrieves the text to display in the value label for a given RecyclerView position.
         *
         * @param position The active RecyclerView position.
         * @return A String containing the text that should be
         */
        fun getValueLabelText(position: Int): String
    }

    /**
     * An enhanced version of RecyclerView.OnScrollListener() that conveys scroll events to
     * a RecyclerViewScrollbar View so that the scrollbar can adjust to changes in scroll position.
     *
     * @property scrollbar The RecyclerViewScrollbar instance that scroll events should be propagated to.
     */
    open class OnScrollListener(private val scrollbar: RecyclerViewScrollbar): RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val contentSize = recyclerView.computeVerticalScrollRange()
            scrollbar.notifyRecyclerViewContentHeightChanged(contentSize)

            val scrollPosition = recyclerView.computeVerticalScrollOffset()
            scrollbar.notifyRecyclerViewScrollPositionChanged(scrollPosition)

            if (recyclerView.adapter is ValueLabelListener) {
                val scrollProportion = scrollPosition.toFloat() / contentSize
                val itemCount = (recyclerView.adapter as RecyclerView.Adapter).itemCount
                val activePosition = (scrollProportion * itemCount).roundToInt()

                val valueLabelText = (recyclerView.adapter as ValueLabelListener)
                    .getValueLabelText(activePosition)
                scrollbar.setValueLabelText(valueLabelText)
            }
        }
    }
}