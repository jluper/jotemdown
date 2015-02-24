package com.DataFinancial.NoteJackal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    public static final String DEBUGTAG = "JFL";
    public static final String HELP_FILE = "JeDHelpImport.txt";
    //public static final String FILESAVED = "FileSaved";
     public static final String PASSPOINTS_SET = "RESET_PASSPOINTS";
    public static final int PHOTO_TAKEN_REQUEST = 2;
    public static final int EDIT_NOTE = 4;
    public static final int BROWSE_GALLERY_REQUEST = 3;
    private static int selectedRow = 0;
    private static sortOption sortColumn = sortOption.COL_PRIORITY;
    protected List<Note> notes = new ArrayList<Note>();
    private File imageFile;
    private ImageButton searchButton;
    private ImageButton sortButton;
    private TextView lblSort;
    private TextView lblNumNotes;
    private DatabaseNotes db = new DatabaseNotes(this);
    private DatabaseReminders dbReminders = new DatabaseReminders(this);
    private String fromHelp = null;
    private String searchText = null;
    private ListView noteList;
    ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.note_yellow);
        actionBar.setTitle(getResources().getString(R.string.main_activity_title));
        actionBar.setDisplayShowTitleEnabled(true);

        lblSort = (TextView) findViewById(R.id.lbl_sort);
        lblSort.setText("Created");

        noteList = (ListView) findViewById(R.id.note_list);

        addSearchButtonListener();
        addSortButtonListener();

        db.createNotesTable();
        dbReminders.createRemindersTable();

        if (db.isNotesTableEmpty()) {
            InputStream in;
            try {
                AssetManager assetManager = getAssets();
                in = assetManager.open(HELP_FILE);
                db.importNotesFromAssets(in);
                in.close();
            } catch (IOException e) {
                Toast.makeText(this, "Exception importing help notes: " + e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        searchText = null;
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            fromHelp = extras.getString("help");
            searchText = (String) getResources().getText(
                    R.string.txt_help_search);

            loadNotes(searchText, DatabaseNotes.COL_ID, "ASC");
        } else {
            loadNotes(null, DatabaseNotes.COL_CREATE_DATE, "DESC");
        }
    }


    private boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }


    public boolean isPrivate() {

        SharedPreferences prefs = getSharedPreferences(LockImageActivity.SHARED_PREF_FILE, MODE_PRIVATE);
        boolean isPrivate = prefs.getBoolean(LockImageActivity.PRIVATE, true);

        return isPrivate;
    }
    @Override
    public void onStart() {
        super.onStart(); // Always call the superclass method first
    }

    @Override
    public void onResume() {
        super.onResume(); // Always call the superclass method first
    }

    public void addSearchButtonListener() {

        searchButton = (ImageButton) findViewById(R.id.searchButton);

        searchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                selectedRow = 0;

                String searchText = ((EditText) findViewById(R.id.searchText)).getText().toString();

                loadNotes(searchText, DatabaseNotes.COL_CREATE_DATE, "DESC");

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
            }
        });

    }

    public void addSortButtonListener() {

        sortButton = (ImageButton) findViewById(R.id.sortButton);

        sortButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                selectedRow = 0;

                String sort = DatabaseNotes.COL_CREATE_DATE;

                String dir = "DESC";

                lblSort = (TextView) findViewById(R.id.lbl_sort);

                switch (sortColumn) {
                    case COL_CREATE_DATE:
                        sortColumn = sortOption.COL_PRIORITY;
                        sort = DatabaseNotes.COL_PRIORITY;
                        lblSort.setText("Priority");
                        break;
                    case COL_EDIT_DATE:
                        sortColumn = sortOption.COL_CREATE_DATE;
                        sort = DatabaseNotes.COL_CREATE_DATE;
                        lblSort.setText("Created");
                        break;
                    case COL_BODY:
                        sortColumn = sortOption.COL_EDIT_DATE;
                        sort = DatabaseNotes.COL_EDIT_DATE;
                        lblSort.setText("Edited");
                        break;
                    case COL_PRIORITY:
                        sortColumn = sortOption.COL_BODY;
                        sort = DatabaseNotes.COL_BODY + " COLLATE NOCASE";
                        lblSort.setText("Content");
                        dir = "ASC";
                        break;
                    default:
                        sortColumn = sortOption.COL_CREATE_DATE;
                }

                loadNotes(null, sort, dir);

            }

        });

    }

    private void loadNotes(String select, String order, String dir) {

        notes = db.getAllNotes(select, order, dir);

        lblNumNotes = (TextView) findViewById(R.id.lbl_num_notes);

        lblNumNotes.setText("(" + Integer.toString(notes.size()) + ")");

        NoteAdapter adapter = new NoteAdapter(this, notes);

        noteList.setAdapter(adapter);

        noteList.setSelectionFromTop(selectedRow, 0);

        noteList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View arg1, int pos,
                                    long arg3) {

                selectedRow = pos;

                Note note = (Note) adapter.getItemAtPosition(pos);

                Intent i = new Intent(MainActivity.this, NewNote.class);
                i.putExtra("id", note.getId());
                i.putExtra("priority", note.getPriority());
                i.putExtra("createDate", note.getCreateDate());
                i.putExtra("editDate", note.getEditDate());
                i.putExtra("body", note.getBody());
                i.putExtra("latitude", note.getLatitude());
                i.putExtra("longitude", note.getLongitude());
                i.putExtra("hasReminder", note.getHasReminder());
                i.putExtra("image", note.getImage());
                startActivityForResult(i, EDIT_NOTE);

            }
        });

    }

    private void loadSavedFile() {
//
//        try {
//            FileInputStream fis = openFileInput(TEXTFILE);
//
//            fis.close();
//        } catch (Exception e) {
//            Toast.makeText(this, "Problem reading file", Toast.LENGTH_LONG).show();
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //Log.d(MainActivity.DEBUGTAG, "Private in onCreateOptionsMenu = " + isPrivate());
        if (isPrivate()) {
            menu.findItem(R.id.menu_disable_privacy).setVisible(true);
            menu.findItem(R.id.menu_enable_privacy).setVisible(false);
            menu.findItem(R.id.menu_lock).setVisible(true);
        } else {
            menu.findItem(R.id.menu_disable_privacy).setVisible(false);
            menu.findItem(R.id.menu_enable_privacy).setVisible(true);
            menu.findItem(R.id.menu_lock).setVisible(false);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Intent i;

        switch (id) {
            case R.id.menu_lock:
                i = new Intent(MainActivity.this, LockImageActivity.class);
                startActivity(i);
                break;
            case R.id.menu_new:
                i = new Intent(MainActivity.this, NewNote.class);
                startActivity(i);
                break;
            case R.id.menu_passpoints_reset:
                setPassPointsSaved(false);
                i = new Intent(MainActivity.this, LockImageActivity.class);
                startActivity(i);
                break;
            case R.id.menu_replace_lock_image:
                ReplaceLockImage();
                break;
            case R.id.menu_password_reset:
                i = new Intent(MainActivity.this, ChangePassword.class);
                startActivity(i);
                break;
            case R.id.menu_disable_privacy:
                SharedPreferences prefs = getSharedPreferences(LockImageActivity.SHARED_PREF_FILE, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(LockImageActivity.PRIVATE, false);
                editor.putBoolean(PASSPOINTS_SET, false);
                editor.apply();
                this.invalidateOptionsMenu();
                break;
            case R.id.menu_enable_privacy:
                prefs = getSharedPreferences(LockImageActivity.SHARED_PREF_FILE, MODE_PRIVATE);
                editor = prefs.edit();
                editor.putBoolean(LockImageActivity.PRIVATE, true);
                editor.putBoolean(PASSPOINTS_SET, false);
                editor.apply();
                this.invalidateOptionsMenu();
                break;
            case R.id.menu_backup:
                i = new Intent(MainActivity.this, BackupNotes.class);
                startActivity(i);
                break;
            case R.id.menu_restore:
                i = new Intent(MainActivity.this, RestoreNotes.class);
                startActivity(i);
                break;
            case R.id.menu_import:
                i = new Intent(MainActivity.this, ImportNotes.class);
                startActivity(i);
                break;
            case R.id.menu_export:
                i = new Intent(MainActivity.this, ExportNotes.class);
                startActivity(i);
                break;
            case R.id.menu_help_main:
                String searchText = (String) getResources().getText(R.string.txt_help_search);
                loadNotes(searchText, DatabaseNotes.COL_ID, "ASC");
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void ReplaceLockImage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.replace_lock_image, null);
        builder.setTitle(R.string.replace_lock_image);
        builder.setView(v);
        builder.setMessage(R.string.replace_image_dialog);

        final AlertDialog dlg = builder.create();
        dlg.show();

        Button takePhoto = (Button) dlg.findViewById(R.id.take_photo);
        Button browseGallery = (Button) dlg.findViewById(R.id.browse_gallery);

        takePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                takePhoto();
            }
        });

        browseGallery.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                browseGallery();

            }
        });

    }

    private void browseGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, BROWSE_GALLERY_REQUEST);
    }

    private void takePhoto() {

        File picsDirectory = getFilesDir();
        imageFile = new File(picsDirectory, getString(R.string.PASSPOINTS_PHOTO));

        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        startActivityForResult(i, PHOTO_TAKEN_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == BROWSE_GALLERY_REQUEST) {
            if (intent != null) {
                String[] columns = {MediaStore.Images.Media.DATA};
                Uri imageUri = intent.getData();
                Cursor cursor = getContentResolver().query(imageUri, columns,
                        null, null, null);

                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(columns[0]);
                String imagePath = cursor.getString(columnIndex);

                cursor.close();

                Uri image = Uri.parse(imagePath);

                Toast.makeText(this, "Gallery result" + image,
                        Toast.LENGTH_LONG).show();

                try {
                    copyImageFile(imagePath);
                } catch (IOException e) {
                    Toast.makeText(this, "Exception creating new lock image from gallery: " + e.toString(), Toast.LENGTH_LONG).show();
                }

                setPassPointsSaved(false);
                Intent i = new Intent(MainActivity.this, LockImageActivity.class);
                startActivity(i);
            } else {
                Toast.makeText(this, "No image  selected.", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == PHOTO_TAKEN_REQUEST) {

            if (intent != null) {
                Bitmap photo = BitmapFactory.decodeFile(imageFile
                        .getAbsolutePath());

                if (photo != null) {
                    setPassPointsSaved(false);
                    Intent i = new Intent(MainActivity.this,
                            LockImageActivity.class);
                    startActivity(i);
                } else {
                    Toast.makeText(this, R.string.PHOTO_NOT_SAVED, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void copyImageFile(String sourcePath) throws IOException {

        File picDirectory = getFilesDir();

        File destinationFile = new File(picDirectory, getString(R.string.PASSPOINTS_PHOTO));

        File sourceFile = new File(sourcePath);

        InputStream in = new FileInputStream(sourceFile);
        OutputStream out = new FileOutputStream(destinationFile);

        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;

        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();

    }

    protected void setPassPointsSaved(boolean state) {

        SharedPreferences prefs = getSharedPreferences(
                LockImageActivity.SHARED_PREF_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MainActivity.PASSPOINTS_SET, state);
        editor.commit();
    }

    private enum sortOption {
        COL_PRIORITY, COL_CREATE_DATE, COL_EDIT_DATE, COL_BODY
    }

}
