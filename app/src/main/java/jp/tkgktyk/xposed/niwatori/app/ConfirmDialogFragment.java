package jp.tkgktyk.xposed.niwatori.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/04/16.
 */
public class ConfirmDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";

    public ConfirmDialogFragment() {
    }

    public static ConfirmDialogFragment newInstance(String title, String message, Bundle extras,
                                                    int requestCode) {
        return newInstance(title, message, extras, null, requestCode);
    }

    public static ConfirmDialogFragment newInstance(String title, String message, Bundle extras,
                                                    Fragment target, int requestCode) {
        Bundle args = extras;
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);

        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        fragment.setArguments(args);
        fragment.setTargetFragment(target, requestCode);
        return fragment;
    }

    private String getTitle() {
        return getArguments().getString(ARG_TITLE);
    }

    private String getMessage() {
        return getArguments().getString(ARG_MESSAGE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getTitle())
                .setMessage(getMessage())
                .setPositiveButton(R.string.already_donated, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        returnToTarget();
                    }
                })
                .setNegativeButton(R.string.back, null)
                .create();
    }

    protected void returnToTarget() {
        OnConfirmedListener listener;
        Fragment target = getTargetFragment();
        if (target != null) {
            listener = (OnConfirmedListener) target;
        } else {
            listener = (OnConfirmedListener) getActivity();
        }
        listener.onConfirmed(getTargetRequestCode(), getArguments());
    }

    public interface OnConfirmedListener {
        void onConfirmed(int requestCode, Bundle extras);
    }
}
