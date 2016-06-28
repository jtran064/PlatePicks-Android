package com.platepicks;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.app.LauncherActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.platepicks.dynamoDB.TableComment;
import com.platepicks.dynamoDB.nosql.CommentDO;
import com.platepicks.util.ConnectionCheck;
import com.platepicks.util.ListItemClass;

import java.io.File;
import java.util.List;

import static com.platepicks.dynamoDB.TableComment.getCommentsFromFoodID;

/**
 * Created by pokeforce on 4/24/16.
 */
public class AboutFoodActivity extends AppCompatActivity implements ImageSaver.OnCompleteListener {

    ListItemClass item;
    boolean isScaled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* Basic setup of which layout we want to use (aboutfood) and toolbar (set as "action bar"
         * so Android puts menu options in it) */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aboutfood);

        item = getIntent().getParcelableExtra("key2");
        item.setClicked(1);

        /* set custom fonts */
        Typeface quicksand = Typeface.createFromAsset(getAssets(), "fonts/Quicksand-Regular.otf");
        Typeface archistico_bold = Typeface.createFromAsset(getAssets(), "fonts/Archistico_Bold.ttf");
        Typeface ham_heaven = Typeface.createFromAsset(getAssets(), "fonts/Hamburger_Heaven.TTF");
        Typeface source_black_it = Typeface.createFromAsset(getAssets(), "fonts/SourceSansPro-BlackIt.otf");

        Typeface source_bold = Typeface.createFromAsset(getAssets(), "fonts/SourceSansPro-Bold.otf");

        TextView bar_name = (TextView) findViewById(R.id.bar_title);
        bar_name.setTypeface(source_bold);

        final TextView restaurant = (TextView) findViewById(R.id.restaurant_name);

        restaurant.setTypeface(source_black_it);
        restaurant.setText(item.getRestaurantName());
        restaurant.setTextSize(0);

        TextView food = (TextView) findViewById(R.id.food_name);
        food.setText(item.getFoodName());

        Typeface source_reg = Typeface.createFromAsset(getAssets(), "fonts/SourceSansPro-Regular.otf");

        TextView street = (TextView) findViewById(R.id.street);
        street.setTypeface(source_reg);
        TextView city = (TextView) findViewById(R.id.city_state);
        city.setTypeface(source_reg);
        TextView zip = (TextView) findViewById(R.id.zip_code);
        zip.setTypeface(source_reg);

        String whole_address = item.getRestaurantAddress();

        int comma_count = 0;
        for (int i = 0; i < whole_address.length(); ++i) {
            char x = whole_address.charAt(i);
            if (x == ',') {
                ++comma_count;
            }
        }

        if (comma_count <= 2) {
            street.setText(whole_address.split("\\,")[0]);
            city.setText(whole_address.split("\\, ")[1]);
            zip.setText(whole_address.split("\\, ")[2]);
        } else {
            street.setText(whole_address.split("\\,")[0] + ',' + whole_address.split("\\,")[1]);
            city.setText(whole_address.split("\\, ")[2]);
            zip.setText(whole_address.split("\\, ")[3]);
        }

        /* "Let's Eat!" text handling */
        final TextView eatBtn = (TextView) findViewById(R.id.eat_button);
        eatBtn.setTypeface(source_bold);

        // Food image
        ImageView img = (ImageView) findViewById(R.id.about_image);
        new ImageSaver(AboutFoodActivity.this).
                setFileName(item.getFoodId()).
                setDirectoryName("images").
                load(img, this);

        /* handle font size for restaurant name */
        isScaled = false;
        final ViewTreeObserver vto = restaurant.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    restaurant.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                if(!isScaled) {
                    LinearLayout ll = (LinearLayout) findViewById(R.id.ll_1);
                    float width = ll.getWidth() - ll.getPaddingRight() - ll.getPaddingLeft();
                    scaleText(restaurant, width);
                    isScaled = true;

                    eatBtn.setWidth(eatBtn.getHeight() + (int) dipToPixels(getBaseContext(), 2));

                    RelativeLayout aboutImage = (RelativeLayout) findViewById(R.id.about_image_frame);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(aboutImage.getWidth(),
                            aboutImage.getWidth() * 6 / 8);

                    aboutImage.setLayoutParams(lp);

                    ImageView foodImage = (ImageView) findViewById(R.id.about_image);
                    foodImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
        });




        // Execute the AsyncTask by passing in foodId
        new QueryCommentsTask(this).execute(item.getFoodId());

        /* handle like/dislike buttons appearing on page */
        RelativeLayout aboutButtons = (RelativeLayout) findViewById(R.id.about_buttons_container);

        if(getIntent().getStringExtra("origin").equals("main page")) {
            aboutButtons.setVisibility(View.VISIBLE);
        }
        else if(getIntent().getStringExtra("origin").equals("about page")) {
            aboutButtons.setVisibility(View.GONE);
        }

        RelativeLayout yesButton = (RelativeLayout) findViewById(R.id.about_button_yes);
        RelativeLayout noButton = (RelativeLayout) findViewById(R.id.about_button_no);

        /* On Click Listeners:
         * Functions that are called whenever the user clicks on the buttons or image */
        noButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    noHeld();
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    noReleased();
                }
                return true;
            }
        });

        yesButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    yesHold();
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    yesReleased();
                }
                return true;
            }
        });
    }
    /* onCreate End */


    /* OnOptionsItemSelected():
     * The function that is called when a menu option is clicked. If true is returned, we should
     * handle the menu click here. If false, Android will try to handle it.*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    public void backArrow(View view) {
        // delete from internal storage
        File dir = getFilesDir();
        File file = new File(dir, item.getFoodId());
        boolean deleted = file.delete();
        finishActivity(0);
        //super.onBackPressed();
    }


    public void openCommentInput(View view) {
        LinearLayout tmp = (LinearLayout) findViewById(R.id.comment_input_field);
        EditText edit = (EditText) findViewById((R.id.input_box));
        if (tmp.getVisibility() == View.GONE) {
            edit.setMaxLines(6);
            edit.setVerticalScrollBarEnabled(true);
            tmp.setVisibility(View.VISIBLE);
            edit.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else
            tmp.setVisibility(View.GONE);

        /* Hide the soft keyboard if necessary */
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(edit.getWindowToken(), 0);

        /* emtpy the EditText view */
        edit.setText("");
    }

    @Override
    public void doSomethingWithBitmap(ImageView imageView, Bitmap b, String foodId) {
        imageView.setImageBitmap(b);
    }

    private void scaleText (TextView s, float width) {
        int i = 1;
        s.setMaxLines(1);

        s.measure(View.MeasureSpec.UNSPECIFIED, s.getWidth());

        while(s.getMeasuredWidth() < width && s.getMeasuredHeight() < dipToPixels(s.getContext(), 50)){
            ++i;
            s.setTextSize(i);
            s.measure(View.MeasureSpec.UNSPECIFIED, s.getWidth());
        }
        --i;
        s.setTextSize(i);

        System.out.println("TextSize = " + Integer.toString(i));
        System.out.println("TextView width = " + Integer.toString(s.getMeasuredWidth()));
    }

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public void goToMaps (View view){
        String address = item.getRestaurantAddress();
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + address);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void yesHold () {
        final FrameLayout yesIcon = (FrameLayout) findViewById(R.id.yes_icon);
        final ImageView yesCircle = (ImageView) findViewById(R.id.yes_circle);
        final ImageView yesShadow = (ImageView) findViewById(R.id.yes_shadow);

        yesIcon.animate().scaleX(0.85f).scaleY(0.85f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        yesCircle.animate().scaleX(0.85f).scaleY(0.85f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        yesShadow.animate().scaleX(0.0f).scaleY(0.0f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());
    }

    public void yesReleased () {
        final FrameLayout yesIcon = (FrameLayout) findViewById(R.id.yes_icon);
        final ImageView yesCircle = (ImageView) findViewById(R.id.yes_circle);
        final ImageView yesShadow = (ImageView) findViewById(R.id.yes_shadow);

        yesCircle.animate().scaleX(1f).scaleY(1f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        yesShadow.animate().scaleX(1f).scaleY(1f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        yesIcon.animate().scaleX(1f).scaleY(1f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        yesIcon.animate().scaleX(1f)
                                .setDuration(400)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        yesIcon.animate().setListener(null);
                                        finishActivity(1);
                                    }
                                });
                    }
                });

    }

    public void noHeld () {
        final ImageView noIcon = (ImageView) findViewById(R.id.no_icon);
        final ImageView noCircle = (ImageView) findViewById(R.id.no_circle);
        final ImageView noShadow = (ImageView) findViewById(R.id.no_shadow);

        noIcon.setRotation(0);

        noIcon.animate().scaleX(0.85f).scaleY(0.85f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        noCircle.animate().scaleX(0.85f).scaleY(0.85f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        noShadow.animate().scaleX(0.0f).scaleY(0.0f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());
    }

    public void noReleased () {

        Log.d("in noRelease", "IN NO RELEASE!!!");

        final ImageView noIcon = (ImageView) findViewById(R.id.no_icon);
        final ImageView noCircle = (ImageView) findViewById(R.id.no_circle);
        final ImageView noShadow = (ImageView) findViewById(R.id.no_shadow);

        noCircle.animate().scaleX(1f).scaleY(1f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        noShadow.animate().scaleX(1f).scaleY(1f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator());

        noIcon.animate().scaleX(1f).scaleY(1f)
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        noIcon.animate().scaleX(1f)
                                .setDuration(400)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        noIcon.animate().setListener(null);
                                        finishActivity(2);
                                    }
                                });
                    }
                });
    }
}

// FIXME: No more comments
class QueryCommentsTask extends AsyncTask<String, Void, List<CommentDO>> {
    AboutFoodActivity activity;

    public QueryCommentsTask(AboutFoodActivity activity) {
        this.activity = activity;
    }
    /** The system calls this to perform work in a worker thread and
     * delivers it the parameters given to AsyncTask.execute() */
    protected List<CommentDO> doInBackground(String... foodId) {
        /*
        if (ConnectionCheck.isConnected(activity))
            return getCommentsFromFoodID(foodId[0]);
        */

        return null;
    }

    /** The system calls this to perform work in the UI thread and delivers
     * the result from doInBackground() */
    protected void onPostExecute(List<CommentDO> result) {
        /*
        for (CommentDO comment : result) {
            activity.loadComments(comment.getContent(), comment.getUserId(), comment.getTime());
        }
        */
    }
}
