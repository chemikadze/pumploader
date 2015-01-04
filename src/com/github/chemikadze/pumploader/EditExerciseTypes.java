package com.github.chemikadze.pumploader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

public class EditExerciseTypes extends Activity {

    public static final String EXERCISE_LIST = "exercise_list";

    ArrayList<String> exercisesList;
    ArrayAdapter<String> exercisesAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_exercise_types);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        View footer = getLayoutInflater().inflate(R.layout.add_exercise_type, null);
        footer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddExercise(v);
            }
        });
        exercisesList = new ArrayList<String>();
        if (getIntent().hasExtra(EXERCISE_LIST)) {
            exercisesList.addAll(getIntent().getStringArrayListExtra(EXERCISE_LIST));
        }
        exercisesAdapter = new ArrayAdapter<String>(this, R.layout.removable_item, R.id.TextView01, exercisesList) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ImageButton removeBtn = (ImageButton)view.findViewById(R.id.remove_exercise_type);
                removeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        remove(getItem(position));
                    }
                });
                return view;
            }
        };
        ListView exercises = (ListView)findViewById(R.id.edit_exercise_list);
        exercises.addFooterView(footer, null, true);
        exercises.setAdapter(exercisesAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_edit_exercise_types, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                onSave(item.getActionView());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onAddExercise(View view) {
        final EditText text = new EditText(this);

        new AlertDialog.Builder(this)
                .setMessage(R.string.add_exercise_type)
                .setView(text)
                .setPositiveButton(R.string.btn_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exercisesAdapter.add(text.getText().toString());
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    public void onSave(View view) {
        Intent data = new Intent();
        data.putStringArrayListExtra(EXERCISE_LIST, exercisesList);
        setResult(RESULT_OK, data);
        finish();
    }

    public void onCancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

}
