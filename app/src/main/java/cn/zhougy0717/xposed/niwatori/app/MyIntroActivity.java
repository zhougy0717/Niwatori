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
                .title(R.string.slide1_title)
                .description(R.string.slide1_description)
                .image(R.drawable.xposed)
                .background(R.color.blue)
                .backgroundDark(R.color.blue_dark)
                .scrollable(false)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide2_title)
                .description(R.string.slide2_description)
                .image(R.drawable.guide_3modes)
                .background(R.color.green)
                .backgroundDark(R.color.green_dark)
                .scrollable(false)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide3_title)
                .description(R.string.slide3_description)
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
                .title(R.string.slide4_title)
                .description(R.string.slide4_description)
                .image(R.drawable.notification_panel)
                .background(R.color.primary_light)
                .backgroundDark(R.color.pink_dark)
                .scrollable(false)
                .build());
        addSlide(new SimpleSlide.Builder()
                .title(R.string.slide5_title)
                .description(R.string.slide5_description)
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
