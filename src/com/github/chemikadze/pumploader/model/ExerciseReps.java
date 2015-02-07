package com.github.chemikadze.pumploader.model;

import java.io.Serializable;

public final class ExerciseReps implements Serializable {

    private int count;
    private int duration;
    private int weight;

    public ExerciseReps(int count, int duration, int weight) {
        this.count = count;
        this.duration = duration;
        this.weight = weight;
    }

    public int getCount() {
        return count;
    }

    public int getDuration() {
        return duration;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExerciseReps that = (ExerciseReps) o;

        if (count != that.count) return false;
        if (duration != that.duration) return false;
        if (weight != that.weight) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + duration;
        result = 31 * result + weight;
        return result;
    }
}
