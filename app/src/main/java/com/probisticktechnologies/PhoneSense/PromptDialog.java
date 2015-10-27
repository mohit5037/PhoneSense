package com.probisticktechnologies.PhoneSense;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

/**
 * helper for Prompt-Dialog creation
 */
public abstract class PromptDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener {
    private final EditText input;

    /**
     * @param context
     * @param title resource id
     * @param message resource id
     */
    public PromptDialog(Context context, String title, String message) {
        super(context);
        setTitle(title);
        setMessage(message);

        input = new EditText(context);
        setView(input);

        setPositiveButton("Ok", this);
        setNegativeButton("Cancel", this);
    }

    /**
     * will be called when "cancel" pressed.
     * closes the dialog.
     * can be overridden.
     * @param dialog
     */


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (onOkClicked(input.getText().toString())) {
                dialog.dismiss();
            }
        } else {
            onCancelClicked();
            dialog.dismiss();
        }
    }

    /**
     * called when "ok" pressed.
     * @param input
     * @return true, if the dialog should be closed. false, if not.
     */
    abstract public boolean onOkClicked(String input);
    abstract public void onCancelClicked();
}