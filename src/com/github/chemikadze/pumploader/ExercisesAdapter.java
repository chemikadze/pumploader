package com.github.chemikadze.pumploader;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import static com.github.chemikadze.pumploader.Utils.twoDigitFormatter;

class ExercisesAdapter extends SimpleExpandableListAdapter {

    public static final String SET_DURATION = "duration";
    public static final String SET_COUNT = "count";
    public static final String EXERCISE_NAME = "name";
    public static final String EXERCISE_COMMENT = "comment";

    private final static String[] exerciseAttrKeys = new String[] { EXERCISE_NAME, EXERCISE_COMMENT };
    private final static int[] exerciseAttrVals = new int[] { R.id.exercise_name, R.id.exercise_comment };

    private final static String[] setAttrKeys = new String[] { SET_COUNT, SET_DURATION };
    private final static int[] setAttrVals = new int[] { R.id.exercise_name, R.id.exercise_duration };


    private ArrayList<HashMap<String, Object>> exercises;
    private ArrayList<ArrayList<HashMap<String, Object>>> sets;

    private Utils.Function1<Integer, View.OnClickListener> onAddClickListenerFactory;

    public ExercisesAdapter(Context context, ArrayList<HashMap<String, Object>> groupData, int groupLayout, ArrayList<ArrayList<HashMap<String, Object>>> childData, int childLayout) {
        super(context, groupData, groupLayout, exerciseAttrKeys, exerciseAttrVals, childData, childLayout, setAttrKeys, setAttrVals);
        this.exercises = groupData;
        this.sets = childData;
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

    public void setOnAddClickListenerFactory(Utils.Function1<Integer, View.OnClickListener> onAddClickListenerFactory) {
        this.onAddClickListenerFactory = onAddClickListenerFactory;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View view = super.getGroupView(groupPosition, isExpanded, convertView, parent);
        Button addButton = (Button)view.findViewById(R.id.add_set);
        addButton.setOnClickListener(onAddClickListenerFactory.apply(groupPosition));
        if (!exercises.get(groupPosition).containsKey(EXERCISE_COMMENT)) {
            view.findViewById(R.id.exercise_comment).setVisibility(View.GONE);
        }
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
