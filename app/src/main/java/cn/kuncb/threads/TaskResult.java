package cn.kuncb.threads;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class TaskResult implements Parcelable {
    private final String name;

    public TaskResult(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected TaskResult(@NotNull Parcel in) {
        name = in.readString();
    }

    public static final Creator<TaskResult> CREATOR = new Creator<TaskResult>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public TaskResult createFromParcel(Parcel in) {
            return new TaskResult(in);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public TaskResult[] newArray(int size) {
            return new TaskResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NotNull Parcel dest, int flags) {
        dest.writeString(name);
    }
}
