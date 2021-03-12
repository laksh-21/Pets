package com.example.pets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pets.data.PetContract;
import com.example.pets.data.PetsDBHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private FloatingActionButton fab;
    private ListView listView;
    private View emptyView;
    private PetsDBHelper mDbHelper;
    private PetCursorAdapter petCursorAdapter;
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private static final int LOADER_ID = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // setting up the floating action button
        fab = (FloatingActionButton)findViewById(R.id.fab_btn);
        listView = (ListView)findViewById(R.id.pets_list_view);
        emptyView = (View)findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);
        mDbHelper = new PetsDBHelper(this);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(MainActivity.this, EditActivity.class);
                startActivity(intent);
            }
        });

        petCursorAdapter = new PetCursorAdapter(this, null);
        listView.setAdapter(petCursorAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                Uri petUri = ContentUris.withAppendedId(PetContract.PetEntry.CONTENT_URI, id);
                intent.setData(petUri);
                startActivity(intent);
            }
        });

        mCallbacks = this;
        getSupportLoaderManager().initLoader(LOADER_ID, null, mCallbacks);

        registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int itemId = item.getItemId();
        switch (itemId){
            case R.id.delete_context_btn:
                showDeletePetConfirmation(info.id);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void deletePet(long id){
        Uri deleteUri = ContentUris.withAppendedId(PetContract.PetEntry.CONTENT_URI, id);
        int deleted = getContentResolver().delete(deleteUri, null, null);
        if(deleted == 0){
            Toast.makeText(this, "Delete was not successful", Toast.LENGTH_SHORT).show();
        } else{
            Toast.makeText(this, "Delete was successful", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    void insertDummyPet(){
        ContentValues values = new ContentValues();
        values.put(PetContract.PetEntry.COLUMN_PET_NAME, "Vito");
        values.put(PetContract.PetEntry.COLUMN_PET_BREED, "Pit/Heeler");
        values.put(PetContract.PetEntry.COLUMN_PET_GENDER, PetContract.PetEntry.GENDER_MALE);
        values.put(PetContract.PetEntry.COLUMN_PET_WEIGHT, 30);

        Uri uri = getContentResolver().insert(PetContract.PetEntry.CONTENT_URI, values);
        if(uri == null){
            Toast.makeText(this, "Could not insert new pet", Toast.LENGTH_SHORT);
            return;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId){
            case R.id.delete_all_btn:
                showDeleteAllConfirmationDialogBox();
                return true;
            case R.id.insert_dummy_btn:
                insertDummyPet();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        String[] projection = {PetContract.PetEntry._ID,
                PetContract.PetEntry.COLUMN_PET_NAME,
                PetContract.PetEntry.COLUMN_PET_BREED};

        return new CursorLoader(this,
                PetContract.PetEntry.CONTENT_URI,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        petCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        petCursorAdapter.swapCursor(null);
    }

    private void showDeleteAllConfirmationDialogBox(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAll();
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

    private void showDeletePetConfirmation(long id){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deletePet(id);
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

    private void deleteAll(){
        int rowsDeleted = getContentResolver().delete(PetContract.PetEntry.CONTENT_URI, null, null);
        if(rowsDeleted == 0){
            Toast.makeText(this, "Delete was not successful", Toast.LENGTH_SHORT).show();
        } else{
            Toast.makeText(this, "Delete was successful", Toast.LENGTH_SHORT).show();
        }
    }
}