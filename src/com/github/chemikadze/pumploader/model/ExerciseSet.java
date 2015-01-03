package com.github.chemikadze.pumploader.model;

public final class ExerciseSet {

    private int count;
    private int duration;

    public ExerciseSet(int count, int duration) {
        this.count = count;
        this.duration = duration;
    }

    public int getCount() {
        return count;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExerciseSet that = (ExerciseSet) o;

        if (count != that.count) return false;
        if (duration != that.duration) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + duration;
        return result;
    }
}
