package org.envirocar.app.views.logbook;

import android.graphics.Canvas;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;


public class LogbookTouchHelper extends ItemTouchHelper.SimpleCallback {

    LogbookItemTouchListener listener;

    public LogbookTouchHelper(int dragDirs, int swipeDirs, LogbookItemTouchListener listener) {
        super(dragDirs, swipeDirs);
        this.listener = listener;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {
            ViewGroup foregroundView = ((LogbookListAdapter.FuelingViewHolder) viewHolder).foregroundView;
            ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(foregroundView);
        }
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        ViewGroup foregroundView = ((LogbookListAdapter.FuelingViewHolder) viewHolder).foregroundView;
        ItemTouchHelper.Callback.getDefaultUIUtil()
                .onDrawOver(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        ViewGroup foregroundView = ((LogbookListAdapter.FuelingViewHolder) viewHolder).foregroundView;

        ItemTouchHelper.Callback.getDefaultUIUtil()
                .onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
        listener.onSwiped((LogbookListAdapter.FuelingViewHolder) viewHolder, viewHolder.getAdapterPosition());

    }

    interface LogbookItemTouchListener {
        void onSwiped(LogbookListAdapter.FuelingViewHolder holder, int position);
    }
}
