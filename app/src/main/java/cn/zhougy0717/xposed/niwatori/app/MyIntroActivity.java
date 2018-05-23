package cn.zhougy0717.xposed.niwatori.app;

import android.content.Intent;
import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import cn.zhougy0717.xposed.niwatori.R;

public class MyIntroActivity extends IntroActivity {
    public static final int REQUEST_CODE_INTRO = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(new SimpleSlide.Builder()
                .title("3 Modes to Try")
                .description("Try it")
        .image(R.drawable.one_handed_modes)
        .background(R.color.primary)
        .backgroundDark(R.color.primary_dark)
                .scrollable(false)
//        .permission(Manifest.permission.CAMERA)
                .build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_CODE_INTRO) {
//            if (resultCode == RESULT_OK) {
//                // Finished the intro
//            } else {
//                // Cancelled the intro. You can then e.g. finish this activity too.
//                finish();
//            }
//        }
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        finish();
        return;
    }
}
