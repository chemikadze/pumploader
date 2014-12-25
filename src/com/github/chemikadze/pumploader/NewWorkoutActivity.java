package com.github.chemikadze.pumploader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.jstrava.connector.JStrava;
import org.jstrava.connector.JStravaV3;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class NewWorkoutActivity extends Activity {

    private static final Integer ADD_ACCOUNT_REQUEST = 1;

    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_EXERCISES = "exercises";
    public static final String SET_DURATION = "duration";
    public static final String SET_COUNT = "count";
    public static final String EXERCISE_NAME = "name";

    public static final String LOG_TAG = NewWorkoutActivity.class.getSimpleName();

    private ExercisesAdapter exerciseAdapter;
    private EditText descriptionWidget;
    private ExpandableListView exerciseListView;

    private ProgressDialog progressDialog;

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
        String[] exerciseAttrKeys = new String[] { EXERCISE_NAME };
        int[] exerciseAttrVals = new int[] { R.id.exercise_name };

        ArrayList<ArrayList<HashMap<String, Object>>> sets = new ArrayList<ArrayList<HashMap<String, Object>>>();
        String[] setAttrKeys = new String[] { SET_COUNT, SET_DURATION };
        int[] setAttrVals = new int[] { R.id.exercise_name, R.id.exercise_duration };

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

        public void addSet(int exerciseId, int count, int elapsed) {
            sets.get(exerciseId).add(newSet(count, elapsed));
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
                    final NumberPicker numberPicker = (NumberPicker)view.findViewById(R.id.set_count_picker);
                    numberPicker.setMinValue(0);
                    numberPicker.setMaxValue(Integer.MAX_VALUE);
                    numberPicker.setWrapSelectorWheel(false);

                    final Chronometer chrono = (Chronometer)view.findViewById(R.id.set_chrono);
                    final AtomicLong elapsed = new AtomicLong(0);
                    final AtomicBoolean running = new AtomicBoolean(false);
                    final AtomicBoolean stopped = new AtomicBoolean(false);

                    final ImageButton startButton = (ImageButton)view.findViewById(R.id.set_chrono_start);
                    startButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (!running.get() && !stopped.get()) {
                                chrono.setBase(SystemClock.elapsedRealtime());
                                chrono.start();
                                running.set(true);
                                startButton.setImageResource(android.R.drawable.ic_media_pause);
                            } else if (running.get() && !stopped.get()) {
                                chrono.stop();
                                elapsed.set((SystemClock.elapsedRealtime() - chrono.getBase()) / 1000);
                                running.set(false);
                                stopped.set(true);
                                startButton.setImageResource(android.R.drawable.ic_media_rew);
                            } else {
                                elapsed.set(0);
                                chrono.setBase(SystemClock.elapsedRealtime());
                                stopped.set(false);
                                startButton.setImageResource(android.R.drawable.ic_media_play);
                            }
                        }
                    });

                    final ImageButton setChronoButton = (ImageButton)view.findViewById(R.id.set_chrono_value);
                    setChronoButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            View picker = NewWorkoutActivity.this.getLayoutInflater().inflate(R.layout.duration_picker, null);
                            final NumberPicker hrsPicker = (NumberPicker)picker.findViewById(R.id.duration_picker_hr);
                            hrsPicker.setMinValue(0);
                            hrsPicker.setMaxValue(100);
                            hrsPicker.setWrapSelectorWheel(false);
                            hrsPicker.setValue((int)elapsed.get() / 60 / 60);
                            final NumberPicker minPicker = (NumberPicker)picker.findViewById(R.id.duration_picker_min);
                            minPicker.setMinValue(0);
                            minPicker.setMaxValue(60);
                            minPicker.setValue((int)elapsed.get() / 60 % 60);
                            minPicker.setWrapSelectorWheel(true);
                            minPicker.setFormatter(twoDigitFormatter);
                            final NumberPicker secPicker = (NumberPicker)picker.findViewById(R.id.duration_picker_sec);
                            secPicker.setMinValue(0);
                            secPicker.setMaxValue(60);
                            secPicker.setValue((int)elapsed.get() % 60);
                            secPicker.setWrapSelectorWheel(true);
                            secPicker.setFormatter(twoDigitFormatter);

                            new AlertDialog.Builder(NewWorkoutActivity.this)
                                    .setTitle(getString(R.string.enter_duration))
                                    .setView(picker)
                                    .setPositiveButton(getString(R.string.btn_set), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            exerciseListView.expandGroup(groupPosition);
                                            elapsed.set(((hrsPicker.getValue() * 60) + minPicker.getValue()) * 60 + secPicker.getValue());
                                            running.set(false);
                                            stopped.set(true);
                                            startButton.setImageResource(android.R.drawable.ic_media_rew);
                                            chrono.stop();
                                            chrono.setBase(SystemClock.elapsedRealtime() - elapsed.get() * 1000);
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.btn_cancel), null)
                                    .show();
                        }
                    });

                    ArrayList<HashMap<String, Object>> currentSets = sets.get(groupPosition);
                    if (!currentSets.isEmpty()) {
                        int lastSetCount = Integer.valueOf(currentSets.get(currentSets.size() - 1).get("count").toString());
                        numberPicker.setValue(lastSetCount);
                    }

                    new AlertDialog.Builder(NewWorkoutActivity.this)
                            .setTitle(getString(R.string.add_set))
                            .setView(view)
                            .setPositiveButton(getString(R.string.btn_add), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    numberPicker.clearFocus();
                                    Integer count = numberPicker.getValue();
                                    exerciseListView.expandGroup(groupPosition);
                                    addSet(groupPosition, count, (int)elapsed.get());
                                }
                            })
                            .setNegativeButton(getString(R.string.btn_cancel), null)
                            .show();
                }
            });
            return view;
        }

        private HashMap<String, Object> newExercise(String name) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(EXERCISE_NAME, name);
            return map;
        }

        private String formatElapsed(int seconds) {
            StringBuilder sb = new StringBuilder(8);
            sb.append(twoDigitFormatter.format(seconds / 60 / 60));
            sb.append(':');
            sb.append(twoDigitFormatter.format(seconds / 60 % 60));
            sb.append(':');
            sb.append(twoDigitFormatter.format(seconds % 60));
            return sb.toString();
        }

        private HashMap<String, Object> newSet(Integer count, Integer elapsed) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(SET_COUNT, String.valueOf(count));
            if (elapsed > 0) {
                map.put(SET_DURATION, formatElapsed(elapsed));
            } else {
                map.put(SET_DURATION, "");
            }
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
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item);
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
            .setNegativeButton(getString(R.string.btn_cancel), null)
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
                        errorDialog(getString(R.string.failed_to_request_account));
                    }
                }
            }, null);
        } else {
            startUpload(as[0]);
        }
    }

    protected String[] getExerciseTypes() {
        return getResources().getStringArray(R.array.default_workout_types);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_ACCOUNT_REQUEST) {
            if (resultCode == RESULT_OK) {
                final AccountManager am = AccountManager.get(getApplicationContext());
                Account[] as = am.getAccountsByType(getString(R.string.account_type));
                if (as.length == 0) {
                    errorDialog(getString(R.string.account_was_not_added));
                } else {
                    startUpload(as[0]);
                }
            } else if (resultCode == NewAccountActivity.RESULT_FAILED) {
                String message = data.getStringExtra(AccountManager.KEY_ERROR_MESSAGE);
                if (message == null) {
                    message = getString(R.string.auth_failed);
                }
                errorDialog(message);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getDescriptionFromConstructor() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < exerciseAdapter.getExercises().size(); ++i) {
            text.append(i + 1);
            text.append(". ");
            text.append(exerciseAdapter.getExercises().get(i).get(EXERCISE_NAME));
            text.append("\n");

            ArrayList<HashMap<String, Object>> currentSets = exerciseAdapter.getSets().get(i);

            for (int j = 0; j < currentSets.size(); ++j) {
                text.append(j + 1);
                text.append(") ");
                text.append(currentSets.get(j).get(SET_COUNT));
                text.append(" times");
                String elapsed = currentSets.get(j).get(SET_DURATION).toString();
                if (!elapsed.isEmpty()) {
                    text.append(", ");
                    text.append(currentSets.get(j).get(SET_DURATION));
                    text.append(" elapsed");
                }
                text.append("\n");
            }
            text.append("\n");
        }
        return text.toString();
    }

    private void startUpload(Account account) {
        EditText titleWidget = (EditText) findViewById(R.id.title_text);
        descriptionWidget = (EditText) findViewById(R.id.description_text);
        CheckBox isPrivateWidget = (CheckBox) findViewById(R.id.is_private);

        String title = titleWidget.getText().toString();
        String descriptionText = descriptionWidget.getText().toString().trim();
        String constructorText = getDescriptionFromConstructor().trim();
        StringBuilder description = new StringBuilder();
        description.append(descriptionText);
        if (!descriptionText.isEmpty() && !constructorText.isEmpty()) {
            description.append("\n\n");
        }
        description.append(constructorText);

        final AccountManager am = AccountManager.get(getApplicationContext());
        Account[] as = am.getAccountsByType(getString(R.string.account_type));
        if (as.length == 0) {
            new AlertDialog.Builder(getApplicationContext())
                    .setMessage("Account was not added, upload cancelled")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NewWorkoutActivity.this.setResult(RESULT_CANCELED);
                            NewWorkoutActivity.this.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            uploadWorkout(as[0], title, description.toString(), isPrivateWidget.isChecked());
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
                    Log.e(LOG_TAG, "Failed to get account token: " + e.getMessage(), e);
                    errorDialog(getString(R.string.failed_get_token));
                    return;
                }
                progressDialog = new ProgressDialog(NewWorkoutActivity.this);
                progressDialog.setTitle(R.string.upload_dialog_title);
                progressDialog.setMessage(description);
                progressDialog.show();
                new UploadActivityTask().execute(token, title, description, isPrivate.toString());
            }
        }, null);
    }

    class UploadActivityTask extends AsyncTask<String, String, Future<Integer>> {
        private String token;
        private String title;
        private String description;
        private Boolean isPrivate;

        @Override
        protected Future<Integer> doInBackground(String... params) {
            token = params[0];
            title = params[1];
            description = params[2];
            isPrivate = Boolean.valueOf(params[3]);
            try {
                JStrava strava = new JStravaV3(token);
                Date date = new Date();
                String dateString = date.toString();
                org.jstrava.entities.activity.Activity a = strava.createActivity(title, "Workout", dateString, 1, description, 0);
                if (a == null) {
                    return new Failure<Integer>(new ExecutionException(getString(R.string.upload_failed_msg), null));
                } else {
                    if (isPrivate) {
                        HashMap<String, Object> activityParams = new HashMap<String, Object>();
                        activityParams.put("private", "1");
                        strava.updateActivity(a.getId(), activityParams);
                    }
                    return new Success<Integer>(a.getId());
                }
            } catch (Exception t) {
                return new Failure<Integer>(new ExecutionException(getString(R.string.upload_unexpected_error), t));
            }
        }

        @Override
        protected void onPostExecute(Future<Integer> r) {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
            try {
                int activityId = r.get();
                if (activityId != 0) {
                    Toast.makeText(getApplicationContext(), R.string.upload_succeeded, Toast.LENGTH_SHORT).show();
                    NewWorkoutActivity.this.setResult(RESULT_OK);
                    NewWorkoutActivity.this.finish();
                }
            } catch (Exception e) {
                Log.i(LOG_TAG, "Failed to upload activity: " + e.getMessage(), e);
                new AlertDialog.Builder(NewWorkoutActivity.this)
                        .setTitle(R.string.upload_failed_title)
                        .setMessage(e.getMessage())
                        .setPositiveButton(R.string.btn_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new UploadActivityTask().execute(token, title, description, isPrivate.toString());
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
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

    // why standard is not visible by compiler?
    final NumberPicker.Formatter twoDigitFormatter = new NumberPicker.Formatter() {
        final StringBuilder mBuilder = new StringBuilder();
        final java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        final Object[] mArgs = new Object[1];
        public String format(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }
    };

    private void errorDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

}