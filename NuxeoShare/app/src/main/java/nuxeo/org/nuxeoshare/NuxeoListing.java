package nuxeo.org.nuxeoshare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.Documents;
import org.nuxeo.client.api.objects.upload.BatchUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NuxeoListing extends AppCompatActivity {

    private NuxeoClient nuxeoClient;
    private Document currentDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuxeo_listing);
        // Get intent, action and MIME type
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        // Prepare nx connection
        SharedPreferences settings = getSharedPreferences(NuxeoShare.PREFS_NAME, 0);
        String login = settings.getString("login", "Administrator");
        String pwd = settings.getString("pwd", "Administrator");
        String url = settings.getString("url", null);
        System.setProperty("log4j2.disable.jmx", "true");
        nuxeoClient = new NuxeoClient(url, login, pwd);
        currentDocument = nuxeoClient.repository().fetchDocumentRoot();

        // Handle listing
        Documents children = currentDocument.fetchChildren();
        final ListView listview = (ListView) findViewById(R.id.nxListView);
        listview.setAdapter(new NuxeoItemAdapter(this, new String[]{"data1",
                "data2"}));

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
            }

        });

        final Button button = (Button) findViewById(R.id.nxbutton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Intent.ACTION_SEND.equals(action) && type != null) {
                    if ("text/plain".equals(type)) {
                        handleSendText(intent); // Handle text being sent
                    } else if (type.startsWith("image/")) {
                        handleSendImage(intent); // Handle single image being sent
                    }
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                    if (type.startsWith("image/")) {
                        handleSendMultipleImages(intent); // Handle multiple images being sent
                    }
                } else {
                    // Handle other intents, such as being started from the home screen
                }
            }
        });
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            String path = getPath(imageUri);
            File file = new File(path);

            // With batchupload
            BatchUpload batchUpload = nuxeoClient.fetchUploadManager();
            batchUpload = batchUpload.upload(file.getName(), file.length(), "", batchUpload.getBatchId(), "1", file);
            Document androidFile = nuxeoClient.repository().fetchDocumentByPath("default-domain/UserWorkspaces/vpasquier/android");
            androidFile.setPropertyValue("file:content", batchUpload.getBatchBlob());
            androidFile.updateDocument();

            // With Automation (but would be with a file already created - it's just for illustration)
            // Blob fileBlob = new Blob(file);
            // fileBlob = nuxeoClient.automation().newRequest("Blob.AttachOnDocument").param("document", "/default-domain/UserWorkspaces/vpasquier/android").input(fileBlob).execute();
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
        }
    }
}