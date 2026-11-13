package moscow.cuteslavicboy.gdzoffline.ui.viewer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import moscow.cuteslavicboy.gdzoffline.GdzApp;
import moscow.cuteslavicboy.gdzoffline.model.TaskRef;
import moscow.cuteslavicboy.gdzoffline.repo.TaskRepository;

public class TaskViewModel extends ViewModel {

    public static final int LOADING = 0;
    public static final int READY = 1;
    public static final int ERROR = 2;

    public static class TaskState {
        public int status = LOADING;
        public TaskRepository.TaskResult result;
        public String error;
    }

    private final MutableLiveData<TaskState> state = new MutableLiveData<>();
    private final TaskRepository repository = new TaskRepository(GdzApp.get());

    public LiveData<TaskState> getState() {
        return state;
    }

    public void load(String bookId, TaskRef ref) {
        TaskState loading = new TaskState();
        loading.status = LOADING;
        state.setValue(loading);
        repository.loadTaskAsync(bookId, ref, new TaskRepository.Callback() {
            @Override
            public void onResult(TaskRepository.TaskResult result) {
                TaskState s = new TaskState();
                s.status = READY;
                s.result = result;
                state.postValue(s);
            }

            @Override
            public void onError(String message) {
                TaskState s = new TaskState();
                s.status = ERROR;
                s.error = message;
                state.postValue(s);
            }
        });
    }
}