package com.riddimon.minesweeper;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends Activity implements OnClickListener {
	private static final String EX_UNCOVERED = "ex_uncovered";
	private static final String EX_MINE_CONFIG = "ex_mine_config";
	private static final String EX_DIFFICULTY = "ex_difficulty";

	private static final int VAL_DIFF_EASY = 1;
	private static final int VAL_DIFF_MEDIUM = 2;
	private static final int VAL_DIFF_HARD = 3;

	int numBlocks = 8;
	int numMines = 10;
	boolean gameOver = false;

	Button mCheat;
	GridLayout mGrid;

	TimerTask mTimer = null;
	static class Block {
		public Block(int r, int c, int numNeighbors) {
			this.r = r;
			this.c = c;
			this.hasMine = false;
			this.uncovered = false;
			this.mines = numNeighbors;
		}
		public Block(int r, int c, boolean mine) {
			this.r = r;
			this.c = c;
			this.hasMine = mine;
			this.uncovered = false;
			this.mines = 0;
		}
		int mines = 0, r = 0, c = 0;
		boolean hasMine = false;
		boolean uncovered = false;
		TextView tv = null;
	}

	Block mBlocks[][];
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_game);
        setupWidgets();
        createGrid(saved);
        renderGrid();
    }

    private void setupWidgets() {
    	mGrid = (GridLayout) findViewById(R.id.grid);
    	mCheat = (Button) findViewById(R.id.cheat);
    	mCheat.setOnClickListener(this);
    }

    private void createGrid(Bundle saved) {
    	mBlocks = new Block[numBlocks][numBlocks];

    	for (int i = 0; i < numBlocks; i++) {
    		for (int j = 0; j < numBlocks; j++) {
    			mBlocks[i][j] = new Block(i, j, false);
    		}
    	}

    	for (int i = 0; i < numMines;) {
    		int block = (int) (Math.floor(Math.random() * (numBlocks * numBlocks)));
    		int row = (block / numBlocks);
    		int column = block % numBlocks;
    		if (!mBlocks[row][column].hasMine) {
    			i++;
    			mBlocks[row][column].hasMine = true;
    			if (row != 0) {
    				mBlocks[row - 1][column].mines++;
        			if (column != 0) {
        				mBlocks[row-1][column-1].mines++;
        			}
        			if (column != numBlocks - 1) {
        				mBlocks[row-1][column+1].mines++;
        			}
    			}
    			if (row != numBlocks - 1) {
    				mBlocks[row + 1][column].mines++;
        			if (column != 0) {
        				mBlocks[row+1][column-1].mines++;
        			}
        			if (column != numBlocks - 1) {
        				mBlocks[row+1][column+1].mines++;
        			}
    			}
    			if (column != 0) {
    				mBlocks[row][column-1].mines++;
    			}
    			if (column != numBlocks - 1) {
    				mBlocks[row][column+1].mines++;
    			}
    		}
    	}
    }

    public void fillview(GridLayout gl) {
        TextView tv;
    	DisplayMetrics metrics = new DisplayMetrics();
    	getWindowManager().getDefaultDisplay().getMetrics(metrics);
    	int gap = (int)(metrics.density * 5);
    	int viewWidth = (mGrid.getWidth() - numBlocks * gap) / numBlocks;
    	int viewHeight = (mGrid.getHeight() - numBlocks * gap) / numBlocks;

        //Stretch buttons
        for(int i = 0 ; i < gl.getChildCount(); i++) {
            tv = (TextView) gl.getChildAt(i);
            tv.setWidth(viewWidth);
            tv.setHeight(viewHeight);
        }
    }

    private void renderGrid() {
    	DisplayMetrics metrics = new DisplayMetrics();
    	getWindowManager().getDefaultDisplay().getMetrics(metrics);
    	final int gap = (int)(metrics.density * 5);
    	mGrid.removeAllViewsInLayout();
    	mGrid.setRowCount(numBlocks);
    	mGrid.setColumnCount(numBlocks);
    	mGrid.setColumnOrderPreserved(true);
    	for (int i = 0; i < numBlocks; i++) {
    		for (int j = 0; j < numBlocks; j++) {
    			TextView tv = new TextView(this);
    			tv.setTextSize(20);
    			tv.setGravity(Gravity.CENTER);
    			//tv.setText(mBlocks[i][j].hasMine ? "Y" : String.valueOf(mBlocks[i][j].mines));
				Block blk = mBlocks[i][j];
				if (blk.uncovered) {
					tv.setBackgroundColor(getResources().getColor(android.R.color.white));
				} else {
					tv.setBackgroundResource(R.drawable.covered);
				}
    			if (blk.uncovered) {
    				if (!blk.hasMine && blk.mines > 0) {
    					tv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    					tv.setText(blk.mines);
    				}
    			}
    			GridLayout.Spec rspec = GridLayout.spec(i, 1);
    			GridLayout.Spec cspec = GridLayout.spec(j, 1);
    			GridLayout.LayoutParams lp = new GridLayout.LayoutParams(rspec, cspec);
    			lp.leftMargin = i == 0 ? 0 : gap;
    			lp.topMargin = j == 0 ? 0 : gap;
    			lp.setGravity(Gravity.FILL);
    			mGrid.addView(tv, lp);
    			tv.setTag(blk);
    			tv.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (gameOver) {
							Toast.makeText(GameActivity.this, R.string
									.game_over_start_a_new_game, Toast.LENGTH_SHORT).show();
							return;
						}
						Block blk = (Block) v.getTag();
						if (!uncover(blk)) {
							// stop game
							Toast.makeText(GameActivity.this, R.string.you_lose
									, Toast.LENGTH_SHORT).show();
			    			v.setBackgroundColor(getResources().getColor(android.R.
			    					color.holo_red_light));
							setGameStatus(true);
							return;
						}
					}
				});
    			blk.tv = tv;
    		}
    	}
    	ViewTreeObserver vto = mGrid.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
        	@Override
        	public void onGlobalLayout() {
                GridLayout gl = (GridLayout) findViewById(R.id.grid);
                fillview(gl);
                ViewTreeObserver obs = gl.getViewTreeObserver();
                obs.removeGlobalOnLayoutListener(this);
        }});
    }

    private void setGameStatus(boolean end) {
    	gameOver = end;
    	invalidateOptionsMenu();
    	mCheat.setVisibility(gameOver ? View.INVISIBLE : View.VISIBLE);
    }

    private boolean uncover(Block b) {
    	if (b.hasMine) {
    		return false;
    	}
    	// dfs around adjacent cells
    	Set<Block> visited = new HashSet<Block>();
    	Stack<Block> s = new Stack<Block>();
    	s.push(b);
    	while (!s.isEmpty()) {
    		Block blk = s.pop();
        	blk.uncovered = true;
			blk.tv.setBackgroundColor(getResources().getColor(android.R.color.white));
			if (blk.mines > 0) {
				blk.tv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
				blk.tv.setText(String.valueOf(blk.mines));
			}
			visited.add(blk);
			if (blk.mines == 0) {
				// explore 4 connected blocks
				if (blk.r != 0) {
					Block nb = mBlocks[blk.r - 1][blk.c];
					if (!nb.hasMine && !visited.contains(nb)) {
						s.push(nb);
					}
				}
				if (blk.r != numBlocks - 1) {
					Block nb = mBlocks[blk.r + 1][blk.c];
					if (!nb.hasMine && !visited.contains(nb)) {
						s.push(nb);
					}
				}
				if (blk.c != 0) {
					Block nb = mBlocks[blk.r][blk.c - 1];
					if (!nb.hasMine && !visited.contains(nb)) {
						s.push(nb);
					}
				}
				if (blk.c != numBlocks - 1) {
					Block nb = mBlocks[blk.r][blk.c + 1];
					if (!nb.hasMine && !visited.contains(nb)) {
						s.push(nb);
					}
				}
			}
    	}
    	return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.game, menu);
    	MenuItem item = menu.add(0, 0, 0, R.string.new_game);
    	item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	item.setIcon(R.drawable.ic_action_replay);
    	if (!gameOver) {
	    	item = menu.add(0, 1, 0, R.string.validate);
	    	item.setIcon(R.drawable.ic_action_accept);
	    	item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	}
        return true;
    }

    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch(item.getItemId()) {
		case 0:
			mCheat.setVisibility(View.VISIBLE);
			setGameStatus(false);
			createGrid(null);
			renderGrid();
			break;
		case 1:
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.cheat:
			//show mines and cover up 8 blocks around each mine after 3 seconds
			break;
		}
	}
}
