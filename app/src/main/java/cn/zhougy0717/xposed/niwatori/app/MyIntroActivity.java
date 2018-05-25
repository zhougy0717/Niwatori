package cn.zhougy0717.xposed.niwatori.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.R;

public class MyIntroActivity extends IntroActivity {
    public static final int REQUEST_CODE_INTRO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(new SimpleSlide.Builder()
                .title("A Xposed Module")
                .description("You have to flash in Xposed Framework. Without it, Niwatori can do nothing.")
                .image(R.drawable.xposed)
                .background(R.color.blue)
                .backgroundDark(R.color.blue_dark)
                .scrollable(false)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title("3 Modes to Try")
                .description("Hook with your favourite shortcut launcher, e.g. fooview, GravityBox and etc.")
                .image(R.drawable.guide_3modes)
                .background(R.color.green)
                .backgroundDark(R.color.green_dark)
                .scrollable(false)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title("Fancy Small Screen Mode")
                .description("The content view can be shrinked. " +
                        "You can also use gestures outside the content to manipulate the view. " +
                        "You can double tap, fling up and down or scroll left and right.")
                .image(R.drawable.small_screen_modes)
                .background(R.color.orange)
                .backgroundDark(R.color.orange_dark)
                .scrollable(false)
                .buttonCtaLabel("Have a try")
                .buttonCtaClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NFW.performAction(MyIntroActivity.this, NFW.ACTION_SMALL_SCREEN);
                    }
                })
                .build());
        addSlide(new SimpleSlide.Builder()
                .title("Notification Panel Applicable")
                .description("Double tap in the shadow. Play fun with shrinked notification panel.")
                .image(R.drawable.notification_panel)
                .background(R.color.primary_light)
                .backgroundDark(R.color.pink_dark)
                .scrollable(false)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title("Explor for more")
                .description("There are bunch of configurations in Settings. Play with it and you will have a better Niwatori.")
                .image(R.drawable.ic_launcher_web)
                .background(R.color.primary)
                .backgroundDark(R.color.primary_dark)
                .scrollable(false)
                .build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = NFW.getSharedPreferences(this);
        prefs.edit().putBoolean("key_first_launch", false).apply();

    }
}
