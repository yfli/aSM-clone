package com.athena.asm;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.athena.asm.Adapter.PostListAdapter;
import com.athena.asm.data.Post;
import com.athena.asm.data.Subject;
import com.athena.asm.util.SmthSupport;
import com.athena.asm.util.StringUtility;
import com.athena.asm.util.task.LoadPostTask;
import com.athena.asm.viewmodel.PostListViewModel;
import com.athena.asm.viewmodel.BaseViewModel;

public class PostListActivity extends Activity implements OnClickListener,
		OnTouchListener, OnLongClickListener, OnGestureListener, BaseViewModel.OnViewModelChangObserver {

	public SmthSupport smthSupport;

	private LayoutInflater inflater;
	
	private PostListViewModel m_viewModel;

	EditText pageNoEditText;
	Button firstButton;
	Button lastButton;
	Button preButton;
	Button goButton;
	Button nextButton;

	TextView titleTextView;

	private int screenHeight;
	private ListView listView;

	private GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.post_list);

		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		smthSupport = SmthSupport.getInstance();
		
		aSMApplication application = (aSMApplication) getApplication();
		m_viewModel = application.postListViewModel();
		m_viewModel.RegisterViewModelChangeObserver(this);

		this.screenHeight = getWindowManager().getDefaultDisplay().getHeight();
		
		Subject newSubject = (Subject) getIntent().getSerializableExtra(
				StringUtility.SUBJECT);
		boolean isNewSubject = m_viewModel.updateSubject(newSubject);
		
		titleTextView = (TextView) findViewById(R.id.title);

		if (HomeActivity.application.isNightTheme()) {
			((LinearLayout) titleTextView.getParent().getParent())
					.setBackgroundColor(getResources().getColor(
							R.color.body_background_night));
		}

		pageNoEditText = (EditText) findViewById(R.id.edittext_page_no);
		pageNoEditText.setText(m_viewModel.currentPageNumber() + "");

		firstButton = (Button) findViewById(R.id.btn_first_page);
		firstButton.setOnClickListener(this);
		lastButton = (Button) findViewById(R.id.btn_last_page);
		lastButton.setOnClickListener(this);
		preButton = (Button) findViewById(R.id.btn_pre_page);
		preButton.setOnClickListener(this);
		goButton = (Button) findViewById(R.id.btn_go_page);
		goButton.setOnClickListener(this);
		nextButton = (Button) findViewById(R.id.btn_next_page);
		nextButton.setOnClickListener(this);

		listView = (ListView) findViewById(R.id.post_list);

		m_viewModel.setBoardType(getIntent().getIntExtra(StringUtility.BOARD_TYPE, 0));
		m_viewModel.setIsToRefreshBoard(false);

		mGestureDetector = new GestureDetector(this);

		if (isNewSubject) {
			LoadPostTask loadPostTask = new LoadPostTask(this, m_viewModel, m_viewModel.currentSubject(),
					0, false, false);
			loadPostTask.execute();
		}
		else {
			reloadPostList();
		}
		// reloadPostList();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// do nothing to stop onCreated
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onDestroy() {
		m_viewModel.UnregisterViewModelChangeObserver();
		
		super.onDestroy();
	}

	public void reloadPostList() {
		if (m_viewModel.postList() == null) {
			
			m_viewModel.ensurePostExists();
			
			firstButton.setEnabled(false);
			preButton.setEnabled(false);
			nextButton.setEnabled(false);
			lastButton.setEnabled(false);
		}

		listView.setAdapter(new PostListAdapter(this, inflater, m_viewModel.postList()));

		m_viewModel.updateCurrentPageNumberFromSubject();
		pageNoEditText.setText(m_viewModel.currentPageNumber() + "");
		listView.requestFocus();

		m_viewModel.setIsPreloadFinished(false);
		m_viewModel.updatePreloadSubjectFromCurrentSubject();

		if (m_viewModel.boardType() == 0) {
			goButton.setVisibility(View.VISIBLE);
			pageNoEditText.setVisibility(View.VISIBLE);
			firstButton.setText(R.string.first_page);
			lastButton.setText(R.string.last_page);
			preButton.setText(R.string.pre_page);
			nextButton.setText(R.string.next_page);

			titleTextView.setText(m_viewModel.subjectTitle());

		} else {
			goButton.setVisibility(View.GONE);
			pageNoEditText.setVisibility(View.GONE);
			firstButton.setText(R.string.topic_first_page);
			lastButton.setText(R.string.topic_all_page);
			preButton.setText(R.string.topic_pre_page);
			nextButton.setText(R.string.topic_next_page);

			titleTextView.setText(m_viewModel.subjectTitle());

		}

		if (m_viewModel.boardType() == 0) {
			int nextPage = m_viewModel.nextPageNumber();
			if (nextPage > 0) {
				m_viewModel.preloadSubject().setCurrentPageNo(nextPage);
				LoadPostTask loadPostTask = new LoadPostTask(this, m_viewModel,
						m_viewModel.preloadSubject(), 0, true, false);
				loadPostTask.execute();
			}
		} else {
			LoadPostTask loadPostTask = new LoadPostTask(this, m_viewModel, m_viewModel.preloadSubject(),
					3, true, false);
			loadPostTask.execute();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			Bundle b = data.getExtras();
			m_viewModel.setIsToRefreshBoard(b.getBoolean(StringUtility.REFRESH_BOARD));
			break;

		default:
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent();

			Bundle b = new Bundle();
			b.putBoolean(StringUtility.REFRESH_BOARD, m_viewModel.isToRefreshBoard());
			i.putExtras(b);

			this.setResult(RESULT_OK, i);
			this.finish();

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public void onClick(View view) {
		boolean isNext = false;
		if (m_viewModel.boardType() == 0) { // 同主题导航

			if (view.getId() == R.id.btn_first_page) {
				m_viewModel.gotoFirstPage();
			} else if (view.getId() == R.id.btn_last_page) {
				m_viewModel.gotoLastPage();
			} else if (view.getId() == R.id.btn_pre_page) {
				m_viewModel.gotoPrevPage();
			} else if (view.getId() == R.id.btn_go_page) {
				int pageSet = Integer.parseInt(pageNoEditText.getText()
						.toString());
				m_viewModel.setCurrentPageNumber(pageSet);
			} else if (view.getId() == R.id.btn_next_page) {
				m_viewModel.gotoNextPage();
				isNext = true;
			}

			m_viewModel.updateSubjectCurrentPageNumberFromCurrentPageNumber();
			pageNoEditText.setText(m_viewModel.currentPageNumber() + "");
			if (view.getParent() != null) {
				((View) view.getParent()).requestFocus();
			}

			LoadPostTask loadPostTask = new LoadPostTask(this, m_viewModel, m_viewModel.currentSubject(),
					0, false, isNext);
			loadPostTask.execute();
		} else {
			int action = 0;
			// int startNumber = 0;
			if (view.getId() == R.id.btn_first_page) {
				action = 1;
			} else if (view.getId() == R.id.btn_pre_page) {
				action = 2;
			} else if (view.getId() == R.id.btn_next_page) {
				action = 3;
				isNext = true;
			} else if (view.getId() == R.id.btn_last_page) {
				m_viewModel.setBoardType(0);
				// startNumber =
				// Integer.parseInt(currentSubject.getSubjectID());
				m_viewModel.updateSubjectIDFromTopicSubjectID();
				m_viewModel.setSubjectCurrentPageNumber(1);
			}
			LoadPostTask loadPostTask = new LoadPostTask(this, m_viewModel, m_viewModel.currentSubject(),
					action, false, isNext);
			loadPostTask.execute();
		}
	}

	private void setListOffset(int jump) {
		int index = listView.getFirstVisiblePosition();
		Log.d("move", String.valueOf(index));
		int newIndex = index + jump;
		if (newIndex == -1) {
			newIndex = 0;
		} else if (listView.getItemAtPosition(newIndex) == null) {
			newIndex = index;
		}
		listView.setSelectionFromTop(newIndex, 0);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mGestureDetector.onTouchEvent(event);
		return false;

	}

	@Override
	public boolean onLongClick(View v) {
		if (smthSupport.getLoginStatus()) {
			RelativeLayout relativeLayout = null;
			if (v.getId() == R.id.PostContent) {
				relativeLayout = (RelativeLayout) v.getParent();
			} else {
				relativeLayout = (RelativeLayout) v;
			}
			final String authorID = (String) ((TextView) relativeLayout
					.findViewById(R.id.AuthorID)).getText();
			final Post post = ((PostListAdapter.ViewHolder)relativeLayout.getTag()).post;
			List<String> itemList = new ArrayList<String>();
			itemList.add(getString(R.string.post_reply_post));
			itemList.add(getString(R.string.post_reply_mail));
			itemList.add(getString(R.string.post_query_author));
			itemList.add(getString(R.string.post_copy_author));
			itemList.add(getString(R.string.post_copy_content));
			itemList.add(getString(R.string.post_foward_self));
			if (post.getAuthor().equals(smthSupport.userid)) {
				itemList.add(getString(R.string.post_edit_post));
			}
			final String[] items = new String[itemList.size()];
			itemList.toArray(items);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.post_alert_title);
			builder.setAdapter(new ArrayAdapter(PostListActivity.this,
                    R.layout.alert_narrow_item, items), new DialogInterface.OnClickListener() {
			//builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					Intent intent;
					switch (item) {
					case 0:
						intent = new Intent();
						intent.setClassName("com.athena.asm",
								"com.athena.asm.WritePostActivity");
						intent.putExtra(
								StringUtility.URL,
								"http://www.newsmth.net/bbspst.php?board="
										+ post.getBoard() + "&reid="
										+ post.getSubjectID());
						intent.putExtra(StringUtility.WRITE_TYPE, WritePostActivity.TYPE_POST);
						intent.putExtra(StringUtility.IS_REPLY, true);
						// activity.startActivity(intent);
						startActivityForResult(intent, 0);
						break;
					case 1:
						intent = new Intent();
						intent.setClassName("com.athena.asm",
								"com.athena.asm.WritePostActivity");
						intent.putExtra(
								StringUtility.URL,
								"http://www.newsmth.net/bbspstmail.php?board="
										+ post.getBoard() + "&id="
										+ post.getSubjectID());
						intent.putExtra(StringUtility.WRITE_TYPE, 1);
						intent.putExtra(StringUtility.IS_REPLY, true);
						startActivity(intent);
						break;
					case 2:
						intent = new Intent();
						intent.setClassName("com.athena.asm",
								"com.athena.asm.ViewProfileActivity");
						intent.putExtra(StringUtility.USERID, authorID);
						startActivity(intent);
						break;
					case 3:
						ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						clip.setText(authorID);
						Toast.makeText(getApplicationContext(),
								"ID ： " + authorID + "已复制到剪贴板",
								Toast.LENGTH_SHORT).show();
						break;
					case 4:
						ClipboardManager clip2 = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						clip2.setText(post.getTextContent());
						Toast.makeText(getApplicationContext(), "帖子内容已复制到剪贴板",
								Toast.LENGTH_SHORT).show();
						break;
					case 5:
						boolean result = smthSupport.forwardPostToMailBox(post);
						if (result) {
							Toast.makeText(getApplicationContext(),
									"已转寄到自己信箱中", Toast.LENGTH_SHORT).show();
						}
						break;
					case 6:
						intent = new Intent();
						intent.setClassName("com.athena.asm",
								"com.athena.asm.WritePostActivity");
						intent.putExtra(
								StringUtility.URL,
								"http://www.newsmth.net/bbsedit.php?board="
										+ post.getBoard() + "&id="
										+ post.getSubjectID() + "&ftype=");
						intent.putExtra(StringUtility.WRITE_TYPE, WritePostActivity.TYPE_POST_EDIT);
						intent.putExtra(StringUtility.TITLE, post.getTitle().replace("主题:", ""));
						startActivityForResult(intent, 0);
					default:
						break;
					}
					dialog.dismiss();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
		return true;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (HomeActivity.application.isTouchScroll()) {
			int touchY = (int) e.getRawY();
			float scale = (float) (screenHeight / 800.0);
			if (touchY > 60 * scale && touchY < 390 * scale) {
				setListOffset(-1);
			} else if (touchY > 410 * scale && touchY < 740 * scale) {
				setListOffset(1);
			}
		}
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		final int flingMinXDistance = 100, flingMaxYDistance = 100;
		if (e1.getX() - e2.getX() > flingMinXDistance
				&& Math.abs(e1.getY() - e2.getY()) < flingMaxYDistance) {
			// Fling left
			Toast.makeText(this, "下一页", Toast.LENGTH_SHORT).show();
			nextButton.performClick();
		} else if (e2.getX() - e1.getX() > flingMinXDistance
				&& Math.abs(e1.getY() - e2.getY()) < flingMaxYDistance) {
			// Fling right
			Toast.makeText(this, "上一页", Toast.LENGTH_SHORT).show();
			preButton.performClick();
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		return;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void OnViewModelChange(BaseViewModel viewModel,
			String changedPropertyName, Object... params) {
		
		if (changedPropertyName.equals(PostListViewModel.POSTLIST_PROPERTY_NAME)) {
			reloadPostList();
		}
		
	}

}
