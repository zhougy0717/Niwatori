package cn.zhougy0717.xposed.niwatori.handlers;

import android.view.MotionEvent;
import android.widget.FrameLayout;

public class StatusBarHandler extends BaseHandler {
    @Override
    protected IFlyingHandler allocateHandler(FrameLayout decorView) {
        return null;
    }

    @Override
    protected IFlyingHandler allocateHandler(Object obj) {
        return null;
    }

    @Override
    protected FrameLayout getDecorView(Object obj) {
        return null;
    }

    private static class CustomizedHandler extends FlyingHandler {

        protected CustomizedHandler(FrameLayout decorView) {
            super(decorView);
        }

        @Override
        protected void actionOnFling() {

        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    }
}
