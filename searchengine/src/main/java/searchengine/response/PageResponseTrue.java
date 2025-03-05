package searchengine.response;

import searchengine.dto.statistics.PageData;

import java.util.List;

public class PageResponseTrue {

    private boolean result;
    private int count;

    private List<PageData> data;


    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<PageData> getData() {
        return data;
    }

    public void setData(List<PageData> data) {
        this.data = data;
    }
}
