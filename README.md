# RecyclerView fast scroller with value label

A Kotlin-based Android library that creates a fast scroller for RecyclerView widgets. The scrollbar features a draggable thumb and customisable value label. A demo application using the scrollbar can be found [here](https://github.com/adam-codersgu/names).

<p align="center">
  <img src="/images/recyclerview-scrollbar-fast-scroller-value-label.gif" width="150" height="300" alt="recyclerview-scrollbar-fast-scroller-value-label.gif">
</p>

## Technical requirements

The library supports Android SDKs 29-33.
The scrollbar is compatible with androidx.recyclerview.widget.RecyclerView widgets.

## Benefits

* Smooth fast-scrolling scrollbar with a draggable thumb and an optional value label.
* The scrollbar is hidden using a fade animation when inactive.
* The scrollbar thumb height adjusts automatically based on the size of the RecyclerView's content.
* The scrollbar thumb has a customisable minimum height, which ensures the thumb does not contract excessively when the RecyclerView content is large (a [reported issue](https://issuetracker.google.com/issues/64729576) with the standard RecyclerView fastscrollEnabled feature)
* Customisable colours and sizes

## User guide

A comprehensive tutorial describing how to use the library can be found on [Coders' Guidebook](https://codersguidebook.com/how-to-create-an-android-app/recyclerview-fast-scroller-with-value-label-library).

### Quick setup

#### Add the following dependency to your module-level build.gradle file

```
implementation 'com.codersguidebook:recyclerview-fastscroller-with-value-label:1.0'
```

#### Add the scrollbar to an XML layout containing the target RecyclerView

```
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />

<com.codersguidebook.recyclerviewfastscroller.RecyclerViewScrollbar
    android:id="@+id/scrollbar"
    android:layout_width="wrap_content"
    android:layout_height="match_parent"
    app:layout_constraintEnd_toEndOf="@id/recyclerView" />
```

#### In your Kotlin code, pass a reference to the RecyclerView to the scrollbar and register an instance of RecyclerViewScrollbar.OnScrollListener to the RecyclerView

```
binding.scrollbar.recyclerView = binding.recyclerView
binding.recyclerView.addOnScrollListener(RecyclerViewScrollbar.OnScrollListener(binding.scrollbar))
```
*N.B. Click [here](https://codersguidebook.com/how-to-create-an-android-app/recyclerview-fast-scroller-with-value-label-library#apply-fastscroller-to-recyclerview) to read how to override RecyclerViewScrollbar.OnScrollListener and add further actions to be performed during RecyclerView scroll events.*

### *Optional* Adding a value label to the scrollbar

#### Ensure your RecyclerView adapter class extends the RecyclerViewScrollbar.ValueLabelListener interface

```
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.codersguidebook.recyclerviewfastscroller.RecyclerViewScrollbar

class NamesAdapter : Adapter<ViewHolder>(), RecyclerViewScrollbar.ValueLabelListener {
```

#### Implement a method called getValueLabelText, which determines the value label text at a given position in the RecyclerView

```
class NamesAdapter : Adapter<ViewHolder>(), RecyclerViewScrollbar.ValueLabelListener {
    var names = listOf<String>()

    // The below implementation will display the first character of the String associated with the scroll position
    override fun getValueLabelText(position: Int): String {
        return names[position][0].uppercase()
    }
```

## Customisable properties of the RecyclerViewScrollbar element

- **thumbAndTrackWidth** The width of the scrollbar track and thumb.
- **thumbMinHeight** The scrollbar thumb will shrink as the size of the content loaded into the RecyclerView increases; however, the thumb will not shrink beyond the minimum height specified by the thumbMinHeight property. The default minimum height is 100dp.
- **thumbOffColor** The colour of the scrollbar thumb when inactive (not selected by the user).
- **thumbOnColor** The colour of the scrollbar thumb when active (selected by the user).
- **trackColor** The colour of the scrollbar track.
- **valueLabelBackgroundColor** The background colour for the value label.
- **valueLabelTextColor** The colour of the value label's text.
- **valueLabelTextSize** The text size used for the value label.
- **valueLabelWidth** The width of the value label. The default width is 200dp.

## Issue reporting and feedback

Issues with the library can be raised by creating a new issue in this GitHub repository. Alternatively, you can send a message using the [contact form](https://codersguidebook.com/contact).
