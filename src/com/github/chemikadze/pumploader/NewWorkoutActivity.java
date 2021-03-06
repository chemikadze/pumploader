package com.github.chemikadze.pumploader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.github.chemikadze.pumploader.model.ExerciseSet;
import com.github.chemikadze.pumploader.model.ExerciseReps;
import org.jstrava.connector.JStrava;
import org.jstrava.connector.JStravaV3;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.chemikadze.pumploader.Utils.formatElapsed;
import static com.github.chemikadze.pumploader.Utils.twoDigitFormatter;

public class NewWorkoutActivity extends Activity {

    private static final Integer ADD_ACCOUNT_REQUEST = 1;
    private static final Integer EDIT_EXERCISES_REQUEST = 2;

    private static final String TAB_TAG_DESCRIPTION = "description";
    private static final String TAB_TAG_EXERCISES = "exercises";

    private static final String STATE_EXERCISE_DATA = "exercises";
    private static final String STATE_TAB = "tab";

    private static final String PREFERENCE_EXERCISE_TYPES = "exercises";

    public static final String LOG_TAG = NewWorkoutActivity.class.getSimpleName();

    private ArrayList<String> exerciseTypes;
    private ExercisesAdapter exerciseAdapter;
    private EditText descriptionWidget;
    private ExpandableListView exerciseListView;
    private ProgressDialog progressDialog;
    private boolean isPrivateSelected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_workout_activity);
        setTitle(R.string.new_activity_title);

        TabHost tabHost = (TabHost)findViewById(R.id.tab_host_input_type);
        tabHost.setup();

        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec(TAB_TAG_EXERCISES);
        spec.setContent(R.id.tab_ex);
        spec.setIndicator(getString(R.string.tab_exersises));
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec(TAB_TAG_DESCRIPTION);
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

        ArrayList<ExerciseSet> sets;
        Object savedSets = null;
        if (savedInstanceState != null) {
            savedSets = savedInstanceState.getSerializable(STATE_EXERCISE_DATA);
        }
        if (savedSets == null) {
            sets = new ArrayList<ExerciseSet>();
        } else {
            sets = (ArrayList<ExerciseSet>)savedSets;
        }

        exerciseAdapter = new ExercisesAdapter(
                getApplicationContext(),
                sets, R.layout.exercise_item,
                R.layout.reps_item);
        exerciseAdapter.setOnAddClickListenerFactory(new Utils.Function1<Integer, View.OnClickListener>() {
            @Override
            View.OnClickListener apply(Integer argument) {
                return new OnAddClickListener(exerciseAdapter, argument);
            }
        });
        exerciseListView.setAdapter(exerciseAdapter);
        registerForContextMenu(exerciseListView);

        EditText titleWidget = (EditText)findViewById(R.id.title_text);
        String prefix = getResources().getString(R.string.new_workout_prefix);
        String dateStr = DateFormat.getDateTimeInstance().format(new Date());
        String caption = prefix + " " + dateStr;
        titleWidget.setText(caption);

        String currentTabTag = TAB_TAG_EXERCISES;

        if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
            descriptionWidget = (EditText)findViewById(R.id.description_text);
            String description = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            currentTabTag = TAB_TAG_DESCRIPTION;
            descriptionWidget.setText(description);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_TAB)) {
            currentTabTag = savedInstanceState.getString(STATE_TAB);
        }

        tabHost.setCurrentTabByTag(currentTabTag);

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        restoreExerciseTypes(preferences);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_is_private:
                item.setChecked(!item.isChecked());
                isPrivateSelected = item.isChecked();
                if (item.isChecked()) {
                    item.setIcon(R.drawable.ic_lock_closed);
                } else {
                    item.setIcon(R.drawable.ic_lock_opened);
                }
                return true;
            case R.id.menu_upload:
                onUploadClicked(item.getActionView());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_EXERCISE_DATA, exerciseAdapter.getExerciseSets());

        TabHost tabHost = (TabHost)findViewById(R.id.tab_host_input_type);
        outState.putString(STATE_TAB, tabHost.getCurrentTabTag());
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

    private void saveExerciseTypes() {
        StringBuilder serialized = new StringBuilder();
        for (int i = 0; i < exerciseTypes.size(); i++) {
            serialized.append(exerciseTypes.get(i));
            if (i != exerciseTypes.size() - 1) {
                serialized.append('\n');
            }
        }
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(PREFERENCE_EXERCISE_TYPES, serialized.toString()).apply();
    }

    public void onAddExercise(View v) {
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item_selected, getExerciseTypes());

        final View view = this.getLayoutInflater().inflate(R.layout.add_exercise_dialog, null);
        final Spinner spinner = (Spinner)view.findViewById(R.id.exercise_type_spinner);
        spinner.setAdapter(spinnerAdapter);
        final ImageButton addButton = (ImageButton)view.findViewById(R.id.add_exercise);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), EditExerciseTypes.class);
                Bundle data = new Bundle();
                data.putStringArrayList(EditExerciseTypes.EXERCISE_LIST, new ArrayList<String>(exerciseTypes));
                intent.putExtras(data);
                startActivityForResult(intent, EDIT_EXERCISES_REQUEST);
            }
        });

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
                        Utils.errorDialog(getApplicationContext(), getString(R.string.failed_to_request_account));
                    }
                }
            }, null);
        } else {
            startUpload(as[0]);
        }
    }

    private void restoreExerciseTypes(SharedPreferences preferences) {
        String serializedExerciseTypes = preferences.getString(PREFERENCE_EXERCISE_TYPES, "");
        exerciseTypes = new ArrayList<String>();
        if (!serializedExerciseTypes.isEmpty()) {
            String[] items = serializedExerciseTypes.split("\n");
            for (int i = 0; i < items.length; i++) {
                exerciseTypes.add(items[i]);
            }

        } else {
            String[] items = getResources().getStringArray(R.array.default_workout_types);
            for (int i = 0; i < items.length; i++) {
                exerciseTypes.add(items[i]);
            }
        }
    }

    protected List<String> getExerciseTypes() {
        return exerciseTypes;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_ACCOUNT_REQUEST) {
            if (resultCode == RESULT_OK) {
                final AccountManager am = AccountManager.get(getApplicationContext());
                Account[] as = am.getAccountsByType(getString(R.string.account_type));
                if (as.length == 0) {
                    Utils.errorToast(getApplicationContext(), getString(R.string.account_was_not_added));
                } else {
                    startUpload(as[0]);
                }
            } else if (resultCode == NewAccountActivity.RESULT_FAILED) {
                String message = data.getStringExtra(AccountManager.KEY_ERROR_MESSAGE);
                if (message == null) {
                    message = getString(R.string.auth_failed);
                }
                Utils.errorToast(getApplicationContext(), message);
            }
        } else if (requestCode == EDIT_EXERCISES_REQUEST) {
            if (resultCode == RESULT_OK) {
                exerciseTypes.clear();
                ArrayList<String> types = data.getStringArrayListExtra(EditExerciseTypes.EXERCISE_LIST);
                exerciseTypes.addAll(types);
                saveExerciseTypes();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getDescriptionFromConstructor() {
        String elapsedFmt = getString(R.string.exercise_elapsed_fmt);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < exerciseAdapter.getExerciseSets().size(); ++i) {
            ExerciseSet exerciseSet = exerciseAdapter.getExerciseSets().get(i);

            text.append(i + 1);
            text.append(". ");
            text.append(exerciseSet.getName());

            if (exerciseSet.getReps().size() > 0) {
                int totalCount = Utils.totalCount(exerciseSet.getReps());
                int totalElapsed = Utils.totalElapsed(exerciseSet.getReps());
                String totalElapsedStr = Utils.formatElapsed(totalElapsed);

                text.append(". ");
                text.append(getResources().getQuantityString(R.plurals.exercise_count_total_fmt, totalCount, totalCount));
                if (totalElapsed > 0) {
                    text.append(String.format(elapsedFmt, totalElapsedStr));
                }
            }
            text.append("\n");

            ArrayList<ExerciseReps> currentSets = exerciseSet.getReps();

            for (int j = 0; j < currentSets.size(); ++j) {
                int count = currentSets.get(j).getCount();
                int elapsed = currentSets.get(j).getDuration();
                int weight = currentSets.get(j).getWeight();

                text.append(j + 1);
                text.append(") ");
                text.append(getResources().getQuantityString(R.plurals.exercise_count_fmt, count, count));
                if (weight > 0) {
                    text.append(" × ");
                    text.append(getResources().getQuantityString(R.plurals.weight_picker_format, weight, weight));
                }
                if (elapsed > 0) {
                    text.append(String.format(elapsedFmt, formatElapsed(currentSets.get(j).getDuration())));
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
        Boolean isPrivate = isPrivateSelected;

        String title = titleWidget.getText().toString();
        String descriptionText = descriptionWidget.getText().toString().trim();
        String constructorText = getDescriptionFromConstructor().trim();
        StringBuilder description = new StringBuilder();
        description.append(descriptionText);
        if (!descriptionText.isEmpty() && !constructorText.isEmpty()) {
            description.append("\n\n");
        }
        description.append(constructorText);
        description.append("\n\n");
        description.append(getString(R.string.export_signature));

        int totalElapsed = 0;
        for (int i = 0; i < exerciseAdapter.getExerciseSets().size(); i++) {
            totalElapsed += Utils.totalElapsed(exerciseAdapter.getExerciseSets().get(i).getReps());
        }

        uploadWorkout(account, title, description.toString(), totalElapsed, isPrivate);
    }

    private void uploadWorkout(Account account, final String title, final String description, final int totalElapsed, final Boolean isPrivate) {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.getAuthToken(account, getString(R.string.account_type), null, null, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                String token;
                try {
                    token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to get account token: " + e.getMessage(), e);
                    Utils.errorDialog(getApplicationContext(), getString(R.string.failed_get_token));
                    return;
                }
                new UploadActivityTask(token, title, description, totalElapsed, isPrivate).execute();
            }
        }, null);
    }

    class UploadActivityTask extends AsyncTask<Void, Void, Future<Integer>> {
        private String token;
        private String title;
        private String description;
        private int totalElapsed;
        private Boolean isPrivate;

        public UploadActivityTask(String token, String title, String description, int totalElapsed, Boolean isPrivate) {
            this.token = token;
            this.title = title;
            this.description = description;
            this.totalElapsed = totalElapsed;
            this.isPrivate = isPrivate;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(NewWorkoutActivity.this);
            progressDialog.setTitle(R.string.upload_dialog_title);
            progressDialog.setMessage(description);
            progressDialog.show();
        }

        @Override
        protected Future<Integer> doInBackground(Void... params) {
            try {
                JStrava strava = new JStravaV3(token);
                Date date = new Date((new Date().getTime() / 1000 - totalElapsed) * 1000);
                String dateString = date.toString();
                org.jstrava.entities.activity.Activity a =
                        strava.createActivity(title, "Workout", dateString, totalElapsed, description, 0);
                if (a == null) {
                    return new Utils.Failure<Integer>(new ExecutionException(getString(R.string.upload_failed_msg), null));
                } else {
                    if (isPrivate) {
                        HashMap<String, Object> activityParams = new HashMap<String, Object>();
                        activityParams.put("private", "1");
                        strava.updateActivity(a.getId(), activityParams);
                    }
                    return new Utils.Success<Integer>(a.getId());
                }
            } catch (Exception t) {
                return new Utils.Failure<Integer>(new ExecutionException(getString(R.string.upload_unexpected_error), t));
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
                                new UploadActivityTask(token, title, description, totalElapsed, isPrivate).execute();
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
            }
        }
    }

    class OnAddClickListener implements View.OnClickListener {
        private ExercisesAdapter exercisesAdapter;
        private final int groupPosition;

        public OnAddClickListener(ExercisesAdapter exercisesAdapter, int groupPosition) {
            this.exercisesAdapter = exercisesAdapter;
            this.groupPosition = groupPosition;
        }

        @Override
        public void onClick(View v) {
            View view = NewWorkoutActivity.this.getLayoutInflater().inflate(R.layout.add_reps_dialog, null);
            final NumberPicker countPicker = (NumberPicker)view.findViewById(R.id.reps_count_picker);
            countPicker.setMinValue(0);
            countPicker.setMaxValue(Integer.MAX_VALUE);
            countPicker.setWrapSelectorWheel(false);
            countPicker.setFormatter(new NumberPicker.Formatter() {
                @Override
                public String format(int value) {
                    return getResources().getQuantityString(R.plurals.exercise_count_fmt, value, value);
                }
            });
            final NumberPicker weightPicker = (NumberPicker)view.findViewById(R.id.reps_weight_picker);
            weightPicker.setMinValue(0);
            weightPicker.setMaxValue(Integer.MAX_VALUE);
            weightPicker.setWrapSelectorWheel(false);
            weightPicker.setFormatter(new NumberPicker.Formatter() {
                @Override
                public String format(int value) {
                    return getResources().getQuantityString(R.plurals.weight_picker_format, value, value);
                }
            });

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
                    minPicker.setMaxValue(59);
                    minPicker.setValue((int)elapsed.get() / 60 % 60);
                    minPicker.setWrapSelectorWheel(true);
                    minPicker.setFormatter(twoDigitFormatter);
                    final NumberPicker secPicker = (NumberPicker)picker.findViewById(R.id.duration_picker_sec);
                    secPicker.setMinValue(0);
                    secPicker.setMaxValue(59);
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

            ArrayList<ExerciseReps> currentReps = exercisesAdapter.getExerciseSets().get(groupPosition).getReps();
            if (!currentReps.isEmpty()) {
                int lastSetCount = currentReps.get(currentReps.size() - 1).getCount();
                int lastWeight = currentReps.get(currentReps.size() - 1).getWeight();
                countPicker.setValue(lastSetCount);
                weightPicker.setValue(lastWeight);
            }

            new AlertDialog.Builder(NewWorkoutActivity.this)
                    .setTitle(getString(R.string.add_reps))
                    .setView(view)
                    .setPositiveButton(getString(R.string.btn_add), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            countPicker.clearFocus();
                            Integer count = countPicker.getValue();
                            Integer weight = weightPicker.getValue();
                            exerciseListView.expandGroup(groupPosition);
                            exercisesAdapter.addSet(groupPosition, count, (int) elapsed.get(), weight);
                        }
                    })
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show();
        }
    }
}
