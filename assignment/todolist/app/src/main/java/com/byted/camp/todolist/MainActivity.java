package com.byted.camp.todolist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.byted.camp.todolist.beans.Note;
import com.byted.camp.todolist.beans.State;
import com.byted.camp.todolist.db.TodoContract;
import com.byted.camp.todolist.db.TodoDbHelper;
import com.byted.camp.todolist.operation.activity.DatabaseActivity;
import com.byted.camp.todolist.operation.activity.DebugActivity;
import com.byted.camp.todolist.operation.activity.SettingActivity;
import com.byted.camp.todolist.ui.NoteListAdapter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.byted.camp.todolist.beans.State.DONE;
import static com.byted.camp.todolist.beans.State.TODO;
import static java.lang.System.out;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "zhusDB";
    private static final int REQUEST_CODE_ADD = 1002;

    private RecyclerView recyclerView;
    private NoteListAdapter notesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, NoteActivity.class),
                        REQUEST_CODE_ADD);
            }
        });

        recyclerView = findViewById(R.id.list_todo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        notesAdapter = new NoteListAdapter(new NoteOperator() {
            @Override
            public void deleteNote(Note note) { MainActivity.this.deleteNote(note); }

            @Override
            public void updateNote(Note note) { MainActivity.this.updateNode(note); }
        });
        recyclerView.setAdapter(notesAdapter);

        notesAdapter.refresh(loadNotesFromDatabase());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingActivity.class));
                return true;
            case R.id.action_debug:
                startActivity(new Intent(this, DebugActivity.class));
                return true;
            case R.id.action_database:
                startActivity(new Intent(this, DatabaseActivity.class));
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD
                && resultCode == Activity.RESULT_OK) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private List<Note> loadNotesFromDatabase() {
        // TODO 从数据库中查询数据，并转换成 JavaBeans
        List<Note> noteList = new ArrayList<Note>();
        TodoDbHelper todoDbHelper=new TodoDbHelper(this);
        SQLiteDatabase db=todoDbHelper.getReadableDatabase();
        String[] projection = {
                BaseColumns._ID,
                TodoContract.TodoEntry.COLUMN_NAME_State,
                TodoContract.TodoEntry.COLUMN_NAME_Date,
                TodoContract.TodoEntry.COLUMN_NAME_content,
                TodoContract.TodoEntry.COLUM_NAME_Priority
        };

        String sortOrder =
                TodoContract.TodoEntry.COLUM_NAME_Priority + " DESC,";

        Cursor cursor = db.query(
                TodoContract.TodoEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );
        Log.i(TAG, "Query：perfrom query data:");
        while (cursor.moveToNext()){
            long Id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            String content = cursor.getString(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_NAME_content));
            String dates = cursor.getString(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_NAME_Date));
            int state = cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_NAME_State));
            String priority = cursor.getString(cursor.getColumnIndex(TodoContract.TodoEntry.COLUM_NAME_Priority));

            Log.i(TAG,"Query：" + "itemId: " + Id + ", content: " + content + ", state: " + state);
            Note note = new Note(Id);
            //list内容
            note.setContent(content);
            //转化时间信息
            try{
                SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
                Date date = dateFormat.parse(dates);
                note.setDate(date);
            }catch (ParseException e){
                e.printStackTrace();
            }
            //转化状态信息
            note.setState(State.from(state));
            note.setPriority(Integer.parseInt(priority));
            noteList.add(note);
        }
        cursor.close();
        return noteList;
    }

    private void deleteNote(Note note) {
        // TODO 删除数据
        TodoDbHelper tododbHelper = new TodoDbHelper(this);
        SQLiteDatabase db = tododbHelper.getWritableDatabase();
        // Define 'where' part of query.
        String selection = BaseColumns._ID + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = {Long.toString(note.id)};
        // Issue SQL statement.
        int deletedRows = db.delete(TodoContract.TodoEntry.TABLE_NAME, selection, selectionArgs);
        notesAdapter.refresh(loadNotesFromDatabase());
        Log.i(TAG, "perform delete data, result:" + deletedRows);
    }

    private void updateNode(Note note) {
        // TODO:更新数据
        TodoDbHelper tododbHelper1 = new TodoDbHelper(this);
        SQLiteDatabase db = tododbHelper1.getWritableDatabase();
        String selection = BaseColumns._ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(note.id)};
        ContentValues values = new ContentValues();
        values.put(TodoContract.TodoEntry.COLUM_NAME_Priority,note.getPriority());
        values.put(TodoContract.TodoEntry.COLUMN_NAME_content,note.getContent());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        String getdate = simpleDateFormat.format(note.getDate());
        values.put(TodoContract.TodoEntry.COLUMN_NAME_Date ,getdate);

        if (note.getState()==State.TODO)
            values.put(TodoContract.TodoEntry.COLUMN_NAME_State, 0);
        else
            values.put(TodoContract.TodoEntry.COLUMN_NAME_State, 1);

        int count = db.update(
                TodoContract.TodoEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        notesAdapter.refresh(loadNotesFromDatabase());
        Log.i(TAG, "perform update data, result:" + count);
    }
}
