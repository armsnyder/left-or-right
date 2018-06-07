package com.armsnyder.leftorright

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    private val leftOrRightIO: LeftOrRightIO by lazy { LeftOrRightIO(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        swipeLayout.setOnRefreshListener(this::refresh)
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        hideArrow()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                refresh()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refresh() {
        if (!swipeLayout.isRefreshing) {
            swipeLayout.isRefreshing = true
        }
        hideArrow()
        launch(UI) {
            try {
                val (direction, minutesUntilArrival, route) = leftOrRightIO.getAdvice()
                arrowImage.setImageResource(directionToImageResource(direction))
                arrowImage.contentDescription = direction.toString().toLowerCase()
                etaText.text = String.format(Locale.ENGLISH, "%d min", minutesUntilArrival)
                routeText.text = route
                showArrow()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                Snackbar.make(coordinatorLayout, R.string.generic_error, Snackbar.LENGTH_LONG)
                        .show()
            }
            swipeLayout.isRefreshing = false
        }
    }

    private fun showArrow() {
        etaText.setTextColor(resources.getColor(R.color.active, null))
        arrowImage.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.active, null))
    }

    private fun hideArrow() {
        etaText.setTextColor(resources.getColor(R.color.plain, null))
        arrowImage.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.plain, null))
    }

    private fun directionToImageResource(direction: Direction): Int {
        return when (direction) {
            Direction.LEFT -> R.drawable.ic_arrow_back_24dp
            Direction.RIGHT -> R.drawable.ic_arrow_forward_24dp
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
