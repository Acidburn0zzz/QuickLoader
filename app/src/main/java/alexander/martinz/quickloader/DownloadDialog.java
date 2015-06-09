/*
 * Copyright 2015 Alexander Martinz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package alexander.martinz.quickloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadDialog extends Activity {
    private EditText mUrl;
    private EditText mFileName;
    private Switch mAutoDetect;

    private Toast mToast;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View v = getLayoutInflater().inflate(R.layout.dialog_download, null, false);

        mUrl = (EditText) v.findViewById(R.id.et_download_url);
        mFileName = (EditText) v.findViewById(R.id.et_download_file_name);

        mAutoDetect = (Switch) v.findViewById(R.id.switch_auto_detect);
        mAutoDetect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                mFileName.setEnabled(!checked);
            }
        });

        final TextView tvAutoDetect = (TextView) v.findViewById(R.id.tv_auto_detect);
        tvAutoDetect.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                mAutoDetect.toggle();
            }
        });

        mUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence cs, int i, int i1, int i2) { }

            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                final String name = ((charSequence != null) ? charSequence.toString() : null);
                final String finalName = extractFilename(name);
                if (finalName != null) {
                    mFileName.setText(finalName);
                }
            }

            @Override public void afterTextChanged(Editable editable) { }
        });

        final Button startDownload = (Button) v.findViewById(R.id.start_download);
        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String url = getText(mUrl);
                if (TextUtils.isEmpty(url)) {
                    showToast(getString(R.string.please_enter_url), false);
                    return;
                }

                String name = getText(mFileName);
                if (TextUtils.isEmpty(name)) {
                    showToast(getString(R.string.filename_must_not_be_empty), false);
                    return;
                }

                final String finalUrl = url;
                final String fileName = name.replace(" ", "_");

                final AlertDialog.Builder builder = new AlertDialog.Builder(DownloadDialog.this);
                builder.setTitle(R.string.verify_url_title)
                        .setMessage(getString(R.string.verify_url_message, url))
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startDownload(finalUrl, fileName);
                                        dialogInterface.dismiss();
                                    }
                                });

                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.download);
        builder.setCancelable(true);
        builder.setView(v);

        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();
    }

    private void startDownload(String url, String fileName) {
        final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName)
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setVisibleInDownloadsUi(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .allowScanningByMediaScanner();

        dm.enqueue(request);
        showToast(getString(R.string.started_download_toast, fileName), true);
    }

    private void showToast(String text, boolean isLong) {
        showToast(text, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
    }

    private void showToast(String text, int length) {
        if (mToast != null) {
            mToast.cancel();
        }

        mToast = Toast.makeText(this, text, length);
        mToast.show();
    }

    private String getText(EditText editText) {
        if (editText == null || editText.getText() == null) {
            return null;
        }

        return editText.getText().toString().trim();
    }

    public static String extractFilename(final String name) {
        if (name == null) {
            return null;
        }

        String finalName = name.trim();
        if (TextUtils.isEmpty(finalName)) {
            return null;
        }

        // split the url and get the last part, which should be the filename
        // example https://example.com/images/new/random_image.png -> random_image.png
        final String[] parts = finalName.split("/");
        if (parts.length < 2) {
            return null;
        }

        finalName = parts[parts.length - 1];

        // if the last part of the url contains a question mark (?) there is most likely the case
        // that a query is attached to the file name, lets get rid of it
        final int index = finalName.indexOf("?");
        if (index != -1) {
            // random_image.png?cdn_timestamp=1058239&session=92990 -> random_image.png
            finalName = finalName.substring(0, index);
        }

        // let's trim it a last time and it should be good to go
        return finalName.trim();
    }

}
