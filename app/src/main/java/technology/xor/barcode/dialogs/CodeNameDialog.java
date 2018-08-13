package technology.xor.barcode.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import technology.xor.barcode.R;

public class CodeNameDialog {

    private SharedPreferences sharedPref;

    public void AlertUser(final Context context) {
        // Get the default URL
        sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        String codeName = sharedPref.getString("code_name", "ALABASTER");

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setTitle("Code Name");
        builder.setMessage("The code name that is used on the cloud server to redirect traffic to the appropriate party");

        final int maxLength = 15;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);

        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(75, 75, 75, 75);

        TextView tv = new TextView(context);
        tv.setTextSize(16);
        tv.setTextColor(context.getResources().getColor(R.color.text));
        tv.setText(R.string.code_name);
        layout.addView(tv);

        final EditText label = new EditText(context);
        label.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        label.setFilters(fArray);
        label.setTextColor(context.getResources().getColor(R.color.text));
        label.setHintTextColor(context.getResources().getColor(R.color.text));
        label.setHint(codeName);
        layout.addView(label);

        builder.setView(layout);
        builder.setPositiveButton("Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {

                        if (label.getText().toString().equals("")) {
                            AlertUser(context);
                            label.setError("Empty code names are not allowed!");
                        } else {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("code_name", label.getText().toString());
                            editor.apply();
                        }
                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setNeutralButton("Clear",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        label.setText("");
                        label.setHint("ALABASTER");
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("code_name", "ALABASTER");
                        editor.apply();
                    }
                });

        label.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                label.setError(null);
                label.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.show();
    }
}

