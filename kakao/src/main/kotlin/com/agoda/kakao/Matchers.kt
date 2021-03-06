package com.agoda.kakao

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.test.espresso.matcher.BoundedMatcher
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.AppCompatDrawableManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * Matches TextView views which contains any text
 *
 * @see TextView
 */
class AnyTextMatcher : BoundedMatcher<View, TextView>(TextView::class.java) {
    override fun describeTo(desc: Description) {
        desc.appendText("which has any text")
    }

    override fun matchesSafely(view: TextView?): Boolean = view?.text?.toString()?.isNotEmpty() ?: false
}

/**
 * Matches RecyclerView descendant at given position in adapter
 *
 * @param parent Matcher of the recycler view
 * @param position Position of item in adapter
 */
class PositionMatcher(private val parent: Matcher<View>, private val position: Int)
    : BoundedMatcher<View, View>(View::class.java) {

    override fun describeTo(desc: Description) {
        desc.appendText("view holder at $position position of recycler view: ")
                .appendDescriptionOf(parent)
    }

    override fun matchesSafely(view: View?): Boolean {
        view?.let {
            if (parent.matches(view.parent) && view.parent is RecyclerView) {
                val holder = (view.parent as RecyclerView).findViewHolderForAdapterPosition(position)
                return holder?.itemView === view
            }
        }

        return false
    }
}

/**
 * Matches first RecyclerView descendant which matches specific matcher
 *
 * @param parent Matcher of the recycler view
 * @param item Matcher of the item in adapter
 */
class ItemMatcher(private val parent: Matcher<View>, private val item: Matcher<View>)
    : BoundedMatcher<View, View>(View::class.java) {

    var position = -1

    override fun describeTo(desc: Description) {
        desc.appendText("view holder: ")
                .appendDescriptionOf(item)
                .appendText("of recycler view: ")
                .appendDescriptionOf(parent)
    }

    override fun matchesSafely(view: View?): Boolean {
        view?.let {
            if (parent.matches(view.parent) && view.parent is RecyclerView) {
                if (item.matches(view)) {
                    position = (view.parent as RecyclerView).getChildAdapterPosition(view)
                    return true
                }
            }
        }

        return false
    }
}

/**
 * Matches first view
 */
class FirstViewMatcher : BoundedMatcher<View, View>(View::class.java) {
    var matched: Boolean = false

    override fun describeTo(desc: Description) {
        desc.appendText("first view")
    }

    override fun matchesSafely(view: View?): Boolean {
        return if (matched) {
            false
        } else {
            matched = true
            true
        }
    }
}

/**
 * Matches ViewPager which page index equals given
 *
 * @param index Index of page
 */
class PageMatcher(private val index: Int) : BoundedMatcher<View, ViewPager>(ViewPager::class.java) {
    override fun describeTo(desc: Description) {
        desc.appendText("with current page index = $index")
    }

    override fun matchesSafely(view: ViewPager?): Boolean = view?.let { it.currentItem == index } ?: false
}

/**
 * Matches index'th view that matches given matcher
 *
 * @param matcher Matcher that have several matching views
 * @param index index of view that must be matched
 */
class IndexMatcher(private val matcher: Matcher<View>, private val index: Int) : TypeSafeMatcher<View>() {
    var currentIndex = 0

    override fun describeTo(desc: Description) {
        desc.appendText("${index}th view with: ")
                .appendDescriptionOf(matcher)
    }

    public override fun matchesSafely(view: View): Boolean {
        return matcher.matches(view) && currentIndex++ == index
    }
}

/**
 * Matches given drawable with current one
 *
 * @param resId Drawable resource to be matched (default is -1)
 * @param drawable Drawable instance to be matched (default is null)
 * @param toBitmap Lambda with custom Drawable -> Bitmap converter (default is null)
 */
class DrawableMatcher(@DrawableRes private val resId: Int = -1, private val drawable: Drawable? = null,
                      private val toBitmap: ((drawable: Drawable) -> Bitmap)? = null)
        : TypeSafeMatcher<View>(View::class.java) {

    override fun describeTo(desc: Description) {
        desc.appendText("with drawable id $resId or provided instance")
    }

    override fun matchesSafely(view: View?): Boolean {
        if (view !is ImageView && drawable == null) {
            return false
        } else

            if (resId < 0 && drawable == null) {
                return (view as ImageView).drawable == null
            }

        return view?.let {
            var expectedDrawable: Drawable? = AppCompatDrawableManager.get().getDrawable(view.context, resId)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && expectedDrawable != null) {
                expectedDrawable = DrawableCompat.wrap(expectedDrawable).mutate()
            }

            if (expectedDrawable == null) {
                return false
            }

            val convertDrawable = drawable ?: (view as ImageView).drawable
            val bitmap = toBitmap?.invoke(convertDrawable) ?: drawableToBitmap(convertDrawable)
            val otherBitmap = toBitmap?.invoke(expectedDrawable) ?: drawableToBitmap(expectedDrawable)

            return bitmap.sameAs(otherBitmap)
        } ?: false
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        var drawable = drawable

        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }

        if (drawable is StateListDrawable) {
            if (drawable.getCurrent() is BitmapDrawable) {
                val bitmapDrawable = drawable.getCurrent() as BitmapDrawable
                if (bitmapDrawable.bitmap != null) {
                    return bitmapDrawable.bitmap
                }
            }
        }

        val bitmap: Bitmap

        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}