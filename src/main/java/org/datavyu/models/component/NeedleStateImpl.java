package org.datavyu.models.component;

public class NeedleStateImpl implements NeedleState {

    private long time;
    private boolean dragged;

    NeedleStateImpl(long time, boolean dragged) {
        this.time = time;
        this.dragged = dragged;
    }

    @Override
    public long getCurrentTime() {
        return time;
    }

    @Override
    public boolean wasDragged() {
        return dragged;
    }
}
