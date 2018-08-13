package technology.xor.barcode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;

import technology.xor.barcode.barcodereader.BarcodeCaptureActivity;
import technology.xor.barcode.barcodereader.dialogs.CodeNameDialog;
import technology.xor.barcode.barcodereader.dialogs.UrlDialog;

public class MainActivity extends AppCompatActivity {

    // use a compound button so either checkbox or switch widgets work.
    private TextView statusMessage;

    private static final int RC_BARCODE_CAPTURE = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusMessage = (TextView) findViewById(R.id.status_message);
        Button scanBtn = (Button) findViewById(R.id.read_barcode);

        statusMessage.setText(R.string.barcode_header);

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // launch barcode activity.
                Intent intent = new Intent(MainActivity.this, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.AutoCapture, true);
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.domain_settings:
                UrlDialog urlDialog = new UrlDialog();
                urlDialog.AlertUser(MainActivity.this);
                return true;
            case R.id.code_settings:
                CodeNameDialog codeNameDialog = new CodeNameDialog();
                codeNameDialog.AlertUser(MainActivity.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    statusMessage.setText(R.string.barcode_success);
                } else {
                    statusMessage.setText(R.string.barcode_failure);
                }
            } else {
                statusMessage.setText(String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}