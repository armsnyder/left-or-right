package com.armsnyder.leftorright;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView arrowImageView;
    private LeftOrRightIO leftOrRightIO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        swipeRefreshLayout = findViewById(R.id.swipe2);
        swipeRefreshLayout.setOnRefreshListener(this::refresh);
        arrowImageView = findViewById(R.id.imageView);
        Toolbar myToolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(myToolbar);
        leftOrRightIO = new LeftOrRightIO(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideArrow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refresh() {
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }
        hideArrow();
        leftOrRightIO.getAdvice().whenComplete((advice, err) -> {
            if (err == null) {
                Direction direction = advice.getDirection();
                arrowImageView.setImageResource(directionToImageResource(direction));
                arrowImageView.setContentDescription(direction.toString().toLowerCase());
                TextView etaView = findViewById(R.id.eta);
                etaView.setText(String.format(Locale.ENGLISH, "%d min", advice.getMinutesUntilArrival()));
                TextView routeView = findViewById(R.id.route);
                routeView.setText(advice.getRoute());
                showArrow();
            } else {
                Log.e(TAG, err.getMessage(), err);
                Snackbar.make(findViewById(R.id.container2), R.string.generic_error,
                        Snackbar.LENGTH_LONG).show();
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void showArrow() {
        TextView etaView = findViewById(R.id.eta);
        etaView.setTextColor(getResources().getColor(R.color.active, null));
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.active, null)));
    }

    private void hideArrow() {
        TextView etaView = findViewById(R.id.eta);
        etaView.setTextColor(getResources().getColor(R.color.plain, null));
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.plain, null)));
    }

    private int directionToImageResource(Direction direction) {
        switch (direction) {
            case LEFT:
                return R.drawable.ic_arrow_back_24dp;
            case RIGHT:
                return R.drawable.ic_arrow_forward_24dp;
            default:
                throw new IllegalStateException();
        }
    }
}
