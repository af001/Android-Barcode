package technology.xor.barcode.barcodereader.dialogs;

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
import android.util.Patterns;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import technology.xor.barcode.R;

public class UrlDialog {

    private SharedPreferences sharedPref;

    public void AlertUser(final Context context) {
        // Get the default URL
        sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        String URL = sharedPref.getString("domain_name", "https://www.duckduckgo.com");

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setTitle("Server Address");
        builder.setMessage("The domain name of the server you are using to host your cloud application:");

        final int maxLength = 300;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);

        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(75, 75, 75, 75);

        TextView tv = new TextView(context);
        tv.setTextSize(16);
        tv.setTextColor(context.getResources().getColor(R.color.text));
        tv.setText(R.string.domain_name);
        layout.addView(tv);

        final EditText label = new EditText(context);
        label.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        label.setFilters(fArray);
        label.setTextColor(context.getResources().getColor(R.color.text));
        label.setHintTextColor(context.getResources().getColor(R.color.text));
        label.setHint(URL);
        layout.addView(label);

        builder.setView(layout);
        builder.setPositiveButton("Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {

                        if (label.getText().toString().equals("")) {
                            AlertUser(context);
                            label.setError("Empty urls are not allowed!");
                        } else {
                            if (IsValidDomain(label.getText().toString())) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("domain_name", label.getText().toString());
                                editor.apply();
                            } else {
                                AlertUser(context);
                                label.setError("Invalid domain name!");
                            }
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
                        label.setHint(R.string.default_url);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("domain_name", "https://www.duckduckgo.com");
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

    private boolean IsValidDomain(String domain) {
        CharSequence target = domain;
        return target != null && Patterns.WEB_URL.matcher(target).matches();
    }
}
