package searchengine.response;

public class FalseResponse {
    private final boolean result = false;
    private String error;

    public FalseResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

