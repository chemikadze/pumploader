package com.github.chemikadze.pumploader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.jstrava.connector.JStrava;
import org.jstrava.connector.JStravaV3;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

public class NewWorkoutActivity extends Activity {

    private static final Integer ADD_ACCOUNT_REQUEST = 1;

    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_EXERCISES = "exercises";

    private final String tag = this.getClass().getSimpleName();

    private ExercisesAdapter exerciseAdapter;
    private EditText descriptionWidget;
    private ExpandableListView exerciseListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_workout_activity);

        TabHost tabHost = (TabHost)findViewById(R.id.tab_host_input_type);
        tabHost.setup();

        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec(TAG_EXERCISES);
        spec.setContent(R.id.tab_ex);
        spec.setIndicator(getString(R.string.tab_exersises));
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec(TAG_DESCRIPTION);
        spec.setContent(R.id.tab_descr);
        spec.setIndicator(getString(R.string.tab_description));
        tabHost.addTab(spec);

        exerciseListView = (ExpandableListView)findViewById(R.id.exersizes);
        LayoutInflater inflater = getLayoutInflater();
        View footer = inflater.inflate(R.layout.add_exercise, null);
        footer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddExercise(v);
            }
        });
        exerciseListView.addFooterView(footer, null, false);

        ArrayList<HashMap<String, Object>> exercises = new ArrayList<HashMap<String, Object>>();
        String[] exerciseAttrKeys = new String[] {"name"};
        int[] exerciseAttrVals = new int[] { R.id.exercise_name };

        ArrayList<ArrayList<HashMap<String, Object>>> sets = new ArrayList<ArrayList<HashMap<String, Object>>>();
        String[] setAttrKeys = new String[] {"name"};
        int[] setAttrVals = new int[] { R.id.exercise_name };

        exerciseAdapter = new ExercisesAdapter(
                getApplicationContext(),
                exercises, R.layout.exercise_item, exerciseAttrKeys, exerciseAttrVals,
                sets, R.layout.set_item, setAttrKeys, setAttrVals);
        exerciseListView.setAdapter(exerciseAdapter);
        registerForContextMenu(exerciseListView);

        EditText titleWidget = (EditText)findViewById(R.id.title_text);
        String prefix = getResources().getString(R.string.new_workout_prefix);
        String dateStr = DateFormat.getDateTimeInstance().format(new Date());
        String caption = prefix + " " + dateStr;
        titleWidget.setText(caption);

        if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
            descriptionWidget = (EditText)findViewById(R.id.description_text);
            String description = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            tabHost.setCurrentTabByTag(TAG_DESCRIPTION);
            descriptionWidget.setText(description);
        } else {
            tabHost.setCurrentTabByTag(TAG_EXERCISES);
        }

    }

    class ExercisesAdapter extends SimpleExpandableListAdapter {

        private ArrayList<HashMap<String, Object>> exercises;
        private ArrayList<ArrayList<HashMap<String, Object>>> sets;

        public ExercisesAdapter(Context context, ArrayList<HashMap<String, Object>> groupData, int groupLayout, String[] groupFrom, int[] groupTo, ArrayList<ArrayList<HashMap<String, Object>>> childData, int childLayout, String[] childFrom, int[] childTo) {
            super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
            exercises = groupData;
            sets = childData;
        }

        public ArrayList<HashMap<String, Object>> getExercises() {
            return exercises;
        }

        public ArrayList<ArrayList<HashMap<String, Object>>> getSets() {
            return sets;
        }

        public void removeExercise(int id) {
            exercises.remove(id);
            sets.remove(id);
            notifyDataSetChanged();
        }

        public void addExercise(String description) {
            exercises.add(newExercise(description));
            sets.add(new ArrayList<HashMap<String, Object>>());
            notifyDataSetChanged();
        }

        public void removeSet(int exerciseId, int setId) {
            sets.get(exerciseId).remove(setId);
            notifyDataSetChanged();
        }

        public void addSet(int exerciseId, int count) {
            sets.get(exerciseId).add(newSet(String.valueOf(count), count));
            notifyDataSetChanged();
        }

        @Override
        public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View view = super.getGroupView(groupPosition, isExpanded, convertView, parent);
            Button addButton = (Button)view.findViewById(R.id.add_set);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View view = NewWorkoutActivity.this.getLayoutInflater().inflate(R.layout.add_set_dialog, null);
                    final NumberPicker picker = (NumberPicker)view.findViewById(R.id.set_count_picker);
                    picker.setMinValue(0);
                    picker.setMaxValue(Integer.MAX_VALUE);
                    picker.setWrapSelectorWheel(false);

                    ArrayList<HashMap<String, Object>> currentSets = sets.get(groupPosition);
                    if (!currentSets.isEmpty()) {
                        int lastSetCount = (Integer)currentSets.get(currentSets.size() - 1).get("count");
                        picker.setValue(lastSetCount);
                    }

                    new AlertDialog.Builder(NewWorkoutActivity.this)
                            .setTitle(getString(R.string.add_set))
                            .setView(view)
                            .setPositiveButton(getString(R.string.btn_add), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Integer count = picker.getValue();
                                    exerciseListView.expandGroup(groupPosition);
                                    addSet(groupPosition, count);
                                }
                            })
                            .setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                }
            });
            return view;
        }

        private HashMap<String, Object> newExercise(String name) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("name", name);
            return map;
        }

        private HashMap<String, Object> newSet(String name, Integer count) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("name", name);
            map.put("count", count);
            return map;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.exercise_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.remove_exercise:
                int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
                int set = ExpandableListView.getPackedPositionChild(info.packedPosition);
                switch (ExpandableListView.getPackedPositionType(info.packedPosition)) {
                    case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
                        exerciseAdapter.removeExercise(group);
                        break;
                    case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
                        exerciseAdapter.removeSet(group, set);
                        break;
                    default:
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void onAddExercise(View v) {
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        spinnerAdapter.addAll(getExerciseTypes());

        final View view = this.getLayoutInflater().inflate(R.layout.add_exercise_dialog, null);
        final Spinner spinner = (Spinner)view.findViewById(R.id.exercise_type_spinner);
        spinner.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_exercise))
            .setView(view)
            .setPositiveButton(getString(R.string.btn_add), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String exercise = spinnerAdapter.getItem(spinner.getSelectedItemPosition());
                    exerciseAdapter.addExercise(exercise);
                }
            })
            .setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    public void onUploadClicked(View view) {
        final AccountManager am = AccountManager.get(getApplicationContext());
        Account[] as = am.getAccountsByType(getString(R.string.account_type));
        if (as.length == 0) {
            am.addAccount(getString(R.string.account_type), null, null, null, null, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        startActivityForResult((Intent)future.getResult().get(AccountManager.KEY_INTENT), ADD_ACCOUNT_REQUEST);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to add account", Toast.LENGTH_LONG).show();
                    }
                }
            }, null);
        } else {
            startUpload();
        }
    }

    protected String[] getExerciseTypes() {
        return getResources().getStringArray(R.array.default_workout_types);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_ACCOUNT_REQUEST) {
            if (resultCode == RESULT_OK) {
                startUpload();
            } else {
                Toast.makeText(getApplicationContext(), "Account was not added, upload cancelled", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getDescriptionFromConstructor() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < exerciseAdapter.getExercises().size(); ++i) {
            text.append(i + 1);
            text.append(". ");
            text.append(exerciseAdapter.getExercises().get(i).get("name"));
            text.append("\n");

            ArrayList<HashMap<String, Object>> currentSets = exerciseAdapter.getSets().get(i);

            for (int j = 0; j < currentSets.size(); ++j) {
                text.append(j + 1);
                text.append(") ");
                text.append(currentSets.get(j).get("name"));
                text.append("\n");
            }
            text.append("\n");
        }
        return text.toString();
    }

    private void startUpload() {
        EditText titleWidget = (EditText)findViewById(R.id.title_text);
        descriptionWidget = (EditText)findViewById(R.id.description_text);
        CheckBox isPrivateWidget = (CheckBox)findViewById(R.id.is_private);

        String title = titleWidget.getText().toString();
        String descriptionText = descriptionWidget.getText().toString().trim();
        String constructorText = getDescriptionFromConstructor().trim();
        Toast.makeText(getApplicationContext(), constructorText, Toast.LENGTH_LONG).show();
        StringBuilder description = new StringBuilder();
        description.append(descriptionText);
        if (!descriptionText.isEmpty() && !constructorText.isEmpty()) {
            description.append("\n\n");
        }
        description.append(constructorText);

        uploadWorkout(title, description.toString(), isPrivateWidget.isChecked());

        setResult(RESULT_OK);
        finish();
    }

    private void uploadWorkout(final String title, final String description, final Boolean isPrivate) {
        final AccountManager am = AccountManager.get(getApplicationContext());
        Account[] as = am.getAccountsByType(getString(R.string.account_type));
        if (as.length == 0) {
            Toast.makeText(getApplicationContext(), "Account was not added, upload cancelled", Toast.LENGTH_SHORT).show();
        } else {
            uploadWorkout(as[0], title, description, isPrivate);
        }
    }

    private void uploadWorkout(Account account, final String title, final String description, final Boolean isPrivate) {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.getAuthToken(account, getString(R.string.account_type), null, null, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                String token;
                try {
                    token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                } catch (Exception e) {
                    String msg = "Failed to get account token: " + e.getMessage();
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    return;
                }
                new UploadActivityTask().execute(token, title, description, isPrivate.toString());
            }
        }, null);
    }

    class UploadActivityTask extends AsyncTask<String, String, Future<Integer>> {
        @Override
        protected Future<Integer> doInBackground(String... params) {
            String token = params[0];
            String title = params[1];
            String description = params[2];
            Boolean isPrivate = Boolean.valueOf(params[3]);

            try {
                JStrava strava = new JStravaV3(token);
                Date date = new Date();
                String dateString = date.toString();
                org.jstrava.entities.activity.Activity a = strava.createActivity(title, "Workout", dateString, 1, description, 0);
                if (isPrivate) {
                    HashMap<String, Object> activityParams = new HashMap<String, Object>();
                    activityParams.put("private", "1");
                    strava.updateActivity(a.getId(), activityParams);
                }
                return new Success<Integer>(a.getId());
            } catch (Exception t) {
                return new Failure<Integer>(new ExecutionException(t.getMessage(), t));
            }
        }

        @Override
        protected void onPostExecute(Future<Integer> r) {
            try {
                Toast.makeText(getApplicationContext(), "Uploaded activity with id " + r.get(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                String msg = "Failed to upload activity: " + e.getMessage();
                Log.i(tag, msg, e);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    abstract class CompletedFuture<T> implements Future<T> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }

    class Success<T> extends CompletedFuture<T> {
        T value;

        Success(T value) { this.value = value; }

        @Override
        public T get() throws InterruptedException, ExecutionException { return this.value; }
    }

    class Failure<T> extends CompletedFuture<T> {
        ExecutionException e;
        Failure(ExecutionException e) { this.e = e; }
        @Override
        public T get() throws InterruptedException, ExecutionException { throw e; }
    }
}