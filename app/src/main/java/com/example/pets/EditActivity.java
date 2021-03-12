package com.example.pets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.pets.data.PetContract.PetEntry;
import com.example.pets.data.PetsDBHelper;

public class EditActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private Spinner genderSpinner;
    private EditText mNameEditText;
    private EditText mBreedEditText;
    private EditText mWeightEditText;
    private PetsDBHelper mDbHelper;
    private Uri mCurrentPetUri = null;
    private int mGender;
    private View.OnTouchListener mOnTouchListener;
    private boolean petHasChanged = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        mNameEditText = (EditText)findViewById(R.id.pet_name_edit);
        mBreedEditText = (EditText)findViewById(R.id.pet_breed_edit);
        mWeightEditText = (EditText)findViewById(R.id.pet_weight_edit);
        genderSpinner = (Spinner)findViewById(R.id.gender_spinner);
        mDbHelper = new PetsDBHelper(this);
        mOnTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                petHasChanged = true;
                return false;
            }
        };
        mNameEditText.setOnTouchListener(mOnTouchListener);
        mBreedEditText.setOnTouchListener(mOnTouchListener);
        mWeightEditText.setOnTouchListener(mOnTouchListener);
        genderSpinner.setOnTouchListener(mOnTouchListener);

        Intent intent = getIntent();
        mCurrentPetUri = intent.getData();
        if(mCurrentPetUri == null){
            setTitle("Add a Pet");
            invalidateOptionsMenu();
        } else{
            setTitle("Edit a Pet");
            getSupportLoaderManager().initLoader(1, null, this);
        }
        // populating the gender spinner
        setupSpinner();
    }

    private void setupSpinner(){
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                                                    R.array.gender_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(spinnerAdapter);
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String)parent.getItemAtPosition(position);
                if(!TextUtils.isEmpty(selection)){
                    if(selection.equals("Male")){
                        mGender = PetEntry.GENDER_MALE; // Male selected
                    } else if(selection.equals("Female")){
                        mGender = PetEntry.GENDER_FEMALE; // Female selected
                    } else{
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown selected
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN; // Unknown selected
            }
        });
    }

    void savePet(){
        ContentValues values = new ContentValues();
        String name = mNameEditText.getText().toString().trim();
        String breed = mBreedEditText.getText().toString().trim();
        int weight;
        try {
            weight = Integer.parseInt(mWeightEditText.getText().toString().trim());
        } catch (NumberFormatException e){
            Toast.makeText(this, "Weight should contain only digits(0-9)", Toast.LENGTH_LONG).show();
            return;
        }
        if (name.equals("")){
            Toast.makeText(this, "Please fill the Name Field", Toast.LENGTH_LONG).show();
            return;
        }
        if (breed.equals("")){
            Toast.makeText(this, "Please fill the Breed Field", Toast.LENGTH_LONG).show();
            return;
        }
        values.put(PetEntry.COLUMN_PET_NAME, name);
        values.put(PetEntry.COLUMN_PET_BREED, breed);
        values.put(PetEntry.COLUMN_PET_GENDER, mGender);
        values.put(PetEntry.COLUMN_PET_WEIGHT, weight);

        if(mCurrentPetUri == null) {
            Uri uri = getContentResolver().insert(PetEntry.CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, "Could not insert new pet", Toast.LENGTH_SHORT);
                return;
            }
            Toast.makeText(this, "Pet added", Toast.LENGTH_SHORT).show();
        } else{
            int rowsAffected = getContentResolver().update(mCurrentPetUri, values, null, null);
            if(rowsAffected == 0){
                Toast.makeText(this, "Could not update pet", Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(this, "Pet update successful", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId){
            case R.id.edit_delete_btn:
                showDeleteConfirmationDialogBox();
                return true;
            case R.id.edit_done_btn:
                savePet();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.edit_delete_btn);
            menuItem.setVisible(false);
        }
        return true;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all pet attributes, define a projection that contains
        // all columns from the pet table
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER,
                PetEntry.COLUMN_PET_WEIGHT };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentPetUri,         // Query the content URI for the current pet
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()){
            String name = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_NAME));
            String breed = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_BREED));
            int gender = data.getInt(data.getColumnIndex(PetEntry.COLUMN_PET_GENDER));
            int weight = data.getInt(data.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT));

            mNameEditText.setText(name);
            mBreedEditText.setText(breed);
            mWeightEditText.setText(""+weight);
            genderSpinner.setSelection(gender);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mNameEditText.setText("");
        mBreedEditText.setText("");
        mWeightEditText.setText("");
        genderSpinner.setSelection(0);
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You have unsaved changes");
        builder.setPositiveButton("Discard", discardButtonClickListener);
        builder.setNegativeButton("Keep Editing", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if(!petHasChanged){
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener onButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        showUnsavedChangesDialog(onButtonClickListener);
    }

    private void showDeleteConfirmationDialogBox(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deletePet();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(dialog != null){
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void deletePet(){
        int rowsDeleted = getContentResolver().delete(mCurrentPetUri, null, null);
        if (rowsDeleted == 0){
            Toast.makeText(this, "Pet delete was not successful", Toast.LENGTH_SHORT).show();
        } else{
            Toast.makeText(this, "Pet delete successful", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}