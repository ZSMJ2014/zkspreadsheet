/* Spreadsheet.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Thu May 17 14:41:33     2007, Created by tomyeh
		Dec 19 10:10:10 2007, modify by Dennis Chen.
}}IS_NOTE

Copyright (C) 2007 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package org.zkoss.zss.ui;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import org.zkoss.json.JSONArray;
import org.zkoss.json.JSONObject;
import org.zkoss.lang.Classes;
import org.zkoss.lang.Library;
import org.zkoss.lang.Objects;
import org.zkoss.lang.Strings;
//import org.zkoss.poi.ss.SpreadsheetVersion;
//import org.zkoss.poi.ss.usermodel.AutoFilter;
//import org.zkoss.poi.ss.usermodel.Cell;
//import org.zkoss.poi.ss.usermodel.CellStyle;
//import org.zkoss.poi.ss.usermodel.Chart;
//import org.zkoss.poi.ss.usermodel.DataValidation;
//import org.zkoss.poi.ss.usermodel.DataValidation.ErrorStyle;
//import org.zkoss.poi.ss.usermodel.FilterColumn;
//import org.zkoss.poi.ss.usermodel.Font;
//import org.zkoss.poi.ss.usermodel.Picture;
//import org.zkoss.poi.ss.usermodel.Row;
//import org.zkoss.poi.ss.usermodel.ZssChartX;
//import org.zkoss.poi.ss.util.CellRangeAddress;
//import org.zkoss.poi.ss.util.CellRangeAddressList;
//import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.util.logging.Log;
import org.zkoss.util.media.AMedia;
import org.zkoss.util.media.Media;
import org.zkoss.util.resource.ClassLocator;
import org.zkoss.util.resource.Labels;
import org.zkoss.xel.Function;
import org.zkoss.xel.FunctionMapper;
import org.zkoss.xel.VariableResolver;
import org.zkoss.xel.XelException;
import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.AuResponse;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zk.ui.ext.render.DynamicMedia;
import org.zkoss.zk.ui.sys.ContentRenderer;
import org.zkoss.zk.ui.util.DesktopCleanup;
import org.zkoss.zss.api.AreaRef;
import org.zkoss.zss.api.CellOperationUtil;
import org.zkoss.zss.api.CellRef;
import org.zkoss.zss.api.IllegalFormulaException;
import org.zkoss.zss.api.Importer;
import org.zkoss.zss.api.Range;
import org.zkoss.zss.api.Ranges;
import org.zkoss.zss.api.impl.ImporterImpl;
import org.zkoss.zss.api.model.Book;
import org.zkoss.zss.api.model.Sheet;
import org.zkoss.zss.api.model.SheetProtection;
import org.zkoss.zss.api.model.impl.BookImpl;
import org.zkoss.zss.api.model.impl.SheetImpl;
import org.zkoss.zss.api.model.impl.SimpleRef;
import org.zkoss.zss.model.CellRegion;
import org.zkoss.zss.model.InvalidModelOpException;
import org.zkoss.zss.model.ModelEvent;
import org.zkoss.zss.model.ModelEventListener;
import org.zkoss.zss.model.ModelEvents;
import org.zkoss.zss.model.SAutoFilter;
import org.zkoss.zss.model.SAutoFilter.NFilterColumn;
import org.zkoss.zss.model.SBook;
import org.zkoss.zss.model.SCell;
import org.zkoss.zss.model.SCell.CellType;
import org.zkoss.zss.model.SCellStyle;
import org.zkoss.zss.model.SCellStyle.Alignment;
import org.zkoss.zss.model.SCellStyle.VerticalAlignment;
import org.zkoss.zss.model.SChart;
import org.zkoss.zss.model.SColumnArray;
import org.zkoss.zss.model.SDataValidation;
import org.zkoss.zss.model.SFont;
import org.zkoss.zss.model.SPicture;
import org.zkoss.zss.model.SRow;
import org.zkoss.zss.model.SSheet;
import org.zkoss.zss.model.sys.formula.EvaluationContributorContainer;
import org.zkoss.zss.range.SImporter;
import org.zkoss.zss.range.SImporters;
import org.zkoss.zss.range.SRange;
import org.zkoss.zss.range.SRanges;
import org.zkoss.zss.ui.au.in.Command;
import org.zkoss.zss.ui.au.out.AuCellFocus;
import org.zkoss.zss.ui.au.out.AuCellFocusTo;
import org.zkoss.zss.ui.au.out.AuDataUpdate;
import org.zkoss.zss.ui.au.out.AuHighlight;
import org.zkoss.zss.ui.au.out.AuInsertRowColumn;
import org.zkoss.zss.ui.au.out.AuMergeCell;
import org.zkoss.zss.ui.au.out.AuRemoveRowColumn;
import org.zkoss.zss.ui.au.out.AuRetrieveFocus;
import org.zkoss.zss.ui.au.out.AuSelection;
import org.zkoss.zss.ui.event.CellAreaEvent;
import org.zkoss.zss.ui.event.CellEvent;
import org.zkoss.zss.ui.event.CellHyperlinkEvent;
import org.zkoss.zss.ui.event.Events;
import org.zkoss.zss.ui.event.SheetDeleteEvent;
import org.zkoss.zss.ui.event.SheetEvent;
import org.zkoss.zss.ui.event.StartEditingEvent;
import org.zkoss.zss.ui.event.StopEditingEvent;
import org.zkoss.zss.ui.impl.ActiveRangeHelper;
import org.zkoss.zss.ui.impl.CellFormatHelper;
import org.zkoss.zss.ui.impl.ComponentEvaluationContributor;
import org.zkoss.zss.ui.impl.DefaultUserActionManagerCtrl;
import org.zkoss.zss.ui.impl.DummyDataValidationHandler;
import org.zkoss.zss.ui.impl.DummyFreezeInfoLoader;
import org.zkoss.zss.ui.impl.DummyUndoableActionManager;
import org.zkoss.zss.ui.impl.Focus;
import org.zkoss.zss.ui.impl.HeaderPositionHelper;
import org.zkoss.zss.ui.impl.HeaderPositionHelper.HeaderPositionInfo;
import org.zkoss.zss.ui.impl.JSONObj;
import org.zkoss.zss.ui.impl.JavaScriptValue;
import org.zkoss.zss.ui.impl.MergeAggregation;
import org.zkoss.zss.ui.impl.MergeAggregation.MergeIndex;
import org.zkoss.zss.ui.impl.MergeMatrixHelper;
import org.zkoss.zss.ui.impl.MergedRect;
import org.zkoss.zss.ui.impl.SequenceId;
import org.zkoss.zss.ui.impl.SimpleCellDisplayLoader;
import org.zkoss.zss.ui.impl.StringAggregation;
import org.zkoss.zss.ui.impl.VoidWidgetHandler;
import org.zkoss.zss.ui.impl.XUtils;
import org.zkoss.zss.ui.impl.undo.AggregatedAction;
import org.zkoss.zss.ui.impl.undo.CellEditTextAction;
import org.zkoss.zss.ui.impl.undo.HideHeaderAction;
import org.zkoss.zss.ui.impl.undo.ResizeHeaderAction;
import org.zkoss.zss.ui.sys.CellDisplayLoader;
import org.zkoss.zss.ui.sys.DataValidationHandler;
import org.zkoss.zss.ui.sys.FreezeInfoLoader;
import org.zkoss.zss.ui.sys.SpreadsheetCtrl;
import org.zkoss.zss.ui.sys.SpreadsheetCtrl.CellAttribute;
import org.zkoss.zss.ui.sys.SpreadsheetInCtrl;
import org.zkoss.zss.ui.sys.SpreadsheetOutCtrl;
import org.zkoss.zss.ui.sys.UndoableAction;
import org.zkoss.zss.ui.sys.UndoableActionManager;
import org.zkoss.zss.ui.sys.UserActionManagerCtrl;
import org.zkoss.zss.ui.sys.WidgetHandler;
import org.zkoss.zss.ui.sys.WidgetLoader;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.impl.XulElement;

/**
 * Spreadsheet is a rich ZK Component to handle EXCEL like behavior, it reads
 * the data from a data model({@link SBook}) and interact with this model by
 * event.<br/>
 * You can assign a Book by {@link #setBook(SBook)} or just set the .xls file
 * location by {@link #setSrc(String)}. You also need to set two attributes to
 * restrict max rows and columns to show on client side by
 * {@link #setMaxrows(int)} and {@link #setMaxcolumns(int)}. <br/>
 * To use Spreadsheet in .zul file, just use <code>&lt;spreadsheet/&gt;</code>
 * tag like any other ZK Components.<br/>
 * An simplest example : <br/>
 * <span
 * style="font-family: courier new; font-size: 10pt;">&nbsp;&nbsp;&nbsp;&nbsp
 * ;<span style="color: rgb(0,128,128);">&lt;</span><span
 * style="color: rgb(63,127,127);">spreadsheet&nbsp;</span><span
 * style="color: rgb(127,0,127);">src</span>=<span
 * style="color: rgb(42,0,255);">
 * &quot;/WEB-INF/xls/my.xls&quot;&nbsp;</span><span
 * style="color: rgb(127,0,127);">maxrows</span>=<span
 * style="color: rgb(42,0,255);">&quot;300&quot;&nbsp;</span><span
 * style="color: rgb(127,0,127);">maxcolumns</span>=<span
 * style="color: rgb(42,0,255);">&quot;80&quot;&nbsp;<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span><span
 * style="color: rgb(127,0,127);">height</span>=<span
 * style="color: rgb(42,0,255);">&quot;400px&quot;&nbsp;</span><span
 * style="color: rgb(127,0,127);">width</span>=<span
 * style="color: rgb(42,0,255);">&quot;90%&quot;&nbsp;</span><span
 * style="color: rgb(0,128,128);">/&gt;</span></span>
 * 
 * 
 * @author dennischen
 */

public class Spreadsheet extends XulElement implements Serializable, AfterCompose {
	private static final Log log = Log.lookup(Spreadsheet.class);

	private static final long serialVersionUID = 1L;
	private static final String ROW_SIZE_HELPER_KEY = "_rowCellSize";
	private static final String COLUMN_SIZE_HELPER_KEY = "_colCellSize";
	private static final String MERGE_MATRIX_KEY = "_mergeRange";
	private static final String ACTIVE_RANGE_KEY = "_activeRange";
	private static final String WIDGET_HANDLER_CLS = "org.zkoss.zss.ui.sys.WidgetHandler.class";
	private static final String WIDGET_LOADERS = "org.zkoss.zss.ui.sys.WidgetLoader.class";
	
//	public static final String TOOLBAR_DISABLED_ACTION = "org.zkoss.zss.ui.ToolbarAction.disabled";
	private static final String USER_ACTION_MANAGER_CTRL_CLS = "org.zkoss.zss.ui.UserActionManagerCtrl.class";
	private static final String UNDOABLE_ACTION_MANAGER_CLS = "org.zkoss.zss.ui.UndoableActionManager.class";
	private static final String CELL_DISPLAY_LOADER_CLS = "org.zkoss.zss.ui.CellDisplayLoader.class";
	private static final String DATA_VALIDATION_HANDLER_CLS = "org.zkoss.zss.ui.DataValidationHandler.class";
	private static final String FREEZE_INFO_LOCADER_CLS = "org.zkoss.zss.ui.FreezeInfoLoader.class";
	private static final String COLOR_PICKER_EX_USED_KEY= "org.zkoss.zss.colorPickerExUsed";
	
	private static final int DEFAULT_TOP_HEAD_HEIGHT = 20;
	private static final int DEFAULT_LEFT_HEAD_WIDTH = 36;
	private static final int DEFAULT_CELL_PADDING = 2;
	private static final int DEFAULT_MAX_ROWS = 20;
	private static final int DEFAULT_MAX_COLUMNS = 10;
	
	private static final int DEFAULT_ROW_HEIGHT = 20;
	
	//For IE8: after 15000 cells, browser become unstable
	//For IE9: after 30000 cells, browser become slow
	//For Chrome and FF 10 : after 60000 cells, browser become slower but may acceptable
	private static final int DEFAULT_MAX_RENDERED_CELL_SIZE = 8000;
	
	transient private SBook _book; // the spreadsheet book

	private String _src; // the src to create an internal book
	transient private SImporter _importer; // the spreadsheet importer
	private int _maxRows = DEFAULT_MAX_ROWS; // how many row of this spreadsheet
	private int _maxColumns = DEFAULT_MAX_COLUMNS; // how many column of this spreadsheet
	
	private int _preloadRowSize = -1; //the number of row to load when receiving the rendering request
	private int _preloadColumnSize = -1; //the number of column to load when receiving the rendering request
	
	transient private SSheet _selectedSheet;
	
	private boolean _hideRowhead; // hide row head
	private boolean _hideColhead; // hide column head*/
	
	private boolean _hideGridlines; //hide gridlines
	private boolean _protectSheet;
	private boolean _showFormulabar;
	private boolean _showToolbar;
	private boolean _showSheetbar;
	
	//TODO undo/redo
	//StateManager stateManager = new StateManager(this);
	
	// editor focus is a public focus api to let developer assign and control focus manually
	private Map<String, Focus> _editorFocuses = new HashMap<String, Focus>(20); //id -> Focus
	
	// friend focus is a private api to control focus between share book;
	private Map<String, Focus> _friendFocuses = new HashMap<String, Focus>(20); //id -> Focus
	private String _selfFocusId;

	private AreaRef _focusArea = new AreaRef(0, 0, 0, 0);
	private AreaRef _selectionArea = new AreaRef(0, 0, 0, 0);
	private AreaRef _visibleArea = new AreaRef();
	private AreaRef _highlightArea = null;

	private WidgetHandler _widgetHandler;

	private List<WidgetLoader> _widgetLoaders;
	
	
	//a cleaner to clean book when detach or desktop cleanup
	private BookCleaner _bookCleaner;
	
	/**
	 * default row height when a sheet is empty
	 */
	private int _defaultRowHeight = DEFAULT_ROW_HEIGHT;

	/**
	 * dynamic css version
	 */
	private int _cssVersion = 0;

	/**
	 * width of left header
	 */
	private int _leftheadWidth = DEFAULT_LEFT_HEAD_WIDTH;

	/**
	 * height of top panel
	 */
	private int _topheadHeight = DEFAULT_TOP_HEAD_HEIGHT;

	/**
	 * cell padding of each cell and header, both on left and right side.
	 */
	private int _cellpadding = DEFAULT_CELL_PADDING;

	/**
	 * customized row and column names.
	 */
	private Map _columnTitles;
	private Map _rowTitles;

	private ModelEventListener _modelEventListener = new InnerModelEventDispatcher();
	
	private InnerVariableResolver _variableResolver = new InnerVariableResolver();
	private InnerFunctionMapper _functionMapper = new InnerFunctionMapper();

	/**
	 * Server side customized column/row id, start with 1,3,5,7. If a
	 * column,which set from client side, the id will be 2,4,6 , check ss.js for
	 * detail
	 */
	private SequenceId _custColId = new SequenceId(-1, 2);
	private SequenceId _custRowId = new SequenceId(-1, 2);
	private SequenceId _updateCellId = new SequenceId(0, 1);// to handle batch
	private SequenceId _updateRangeId = new SequenceId(0, 1);
	private SequenceId _focusId = new SequenceId(0, 1);

	private String _userName;
	
	private boolean _clientCacheDisabled = isDefaultClientCacheDisabled();
	
	private static Boolean _defClientCache;
	
	private int _maxRenderedCellSize = getDefaultMaxRenderedCellSize();
	
	private static Integer _defMaxRenderedCellSize;
	
	private Set<AuxAction> _actionDisabled = new HashSet();
//	
//	private static Set<UserAction> _defToolbarActiobDisabled;
	
	private UndoableActionManager _undoableActionManager = null;
	
	private CellDisplayLoader _cellDisplayLoader = null;
	
	private DataValidationHandler _dataValidationHandler = null;
	
	private FreezeInfoLoader _freezeInfoLoader = null;
	
	private static final String ON_MODEL_FRIEND_FOCUS_DELETE = "onFriendFocusDelete";
	private static final String ON_MODEL_FRIEND_FOCUS_MOVE = "onFriendFocusMove";
	
	public Spreadsheet() {
		this.addEventListener("onStartEditingImpl", new EventListener() {
			public void onEvent(Event event) throws Exception {
				Object[] data = (Object[]) event.getData();
				processStartEditing((String) data[0],
						(StartEditingEvent) data[1], (String) data[2]);
			}
		});
		this.addEventListener("onStopEditingImpl", new EventListener() {
			public void onEvent(Event event) throws Exception {

				Object[] data = (Object[]) event.getData();
				processStopEditing((String) data[0], (StopEditingEvent) data[1], (String) data[2]);
			}
		});
		
		initComponentActionHandler();
	}
	
	/**
	 * Gets the user action manager, then you can register/override your custom action by call {@link UserActionManager#registerHandler(String, String, UserActionHandler)}
	 * @return {@link UserActionManager} or null if doesn't support to override 
	 * @since 3.0.0
	 */
	public UserActionManager getUserActionManager(){
		UserActionManagerCtrl uamc = getUserActionManagerCtrl();
		if(uamc instanceof UserActionManager){
			return (UserActionManager)uamc;
		}
		return null;
	}
	
	private Set<String> _lastUAEvents;
	private boolean _ctrlKeysSet;
	private EventListener _uAEventDispatcher;
	
	private void initComponentActionHandler() {
		if(_uAEventDispatcher!=null && _lastUAEvents!=null){
			for(String evt:_lastUAEvents){
				this.removeEventListener(evt, _uAEventDispatcher);
			}
		}
		UserActionManagerCtrl ua = this.getUserActionManagerCtrl();
		_lastUAEvents = ua.getInterestedEvents();
		if(_lastUAEvents!=null && _lastUAEvents.size()>0){
			_uAEventDispatcher = new EventListener() {
				public void onEvent(Event event) throws Exception {
					UserActionManagerCtrl ua = getUserActionManagerCtrl();
					if(ua instanceof EventListener){
						((EventListener)ua).onEvent(event);
					}
				}
			};
			for(String evt:_lastUAEvents){
				this.addEventListener(evt, _uAEventDispatcher);
			}
		}else{
			_lastUAEvents = null;
			_uAEventDispatcher = null;
		}
		
		if(!_ctrlKeysSet){
			String ctrlKeys = ua.getCtrlKeys();
			if(ctrlKeys!=null){//null, don't set, keep the original
				super.setCtrlKeys(ctrlKeys);
			}
		}
	}
	
	private void setUserActionManagerCtrl(UserActionManagerCtrl actionManagerCtrl) {
		if(!Objects.equals(_actionManagerCtrl,actionManagerCtrl)){
			_actionManagerCtrl = actionManagerCtrl;
			_actionManagerCtrl.bind(this);
			initComponentActionHandler();
		}
	}
	
	private UserActionManagerCtrl getUserActionManagerCtrl() {
		if (_actionManagerCtrl == null) {
			String cls = (String) getAttribute(USER_ACTION_MANAGER_CTRL_CLS,true);
			
			if(cls==null){
				cls = (String) Library.getProperty(USER_ACTION_MANAGER_CTRL_CLS);
			}
			if (cls != null) {
				try {
					_actionManagerCtrl = (UserActionManagerCtrl) Classes.newInstance(cls, null, null);
				} catch (Exception x) {
					throw new UiException(x);
				}
			} else {
				_actionManagerCtrl = new DefaultUserActionManagerCtrl();
			}
			_actionManagerCtrl.bind(this);
		}
		return _actionManagerCtrl;
	}
	
	public void setCtrlKeys(String ctrlKeys){
		if(!_ctrlKeysSet && !Objects.equals(getCtrlKeys(),ctrlKeys)){
			_ctrlKeysSet = true;	
		}
		super.setCtrlKeys(ctrlKeys);
	}

	private static boolean isDefaultClientCacheDisabled() {
		if (_defClientCache == null)
			_defClientCache = Boolean.valueOf(Library.getProperty("org.zkoss.zss.spreadsheet.clientcache.disabed", "false"));
		return _defClientCache;
	}

	private static int getDefaultMaxRenderedCellSize() {
		if (_defMaxRenderedCellSize == null)
			_defMaxRenderedCellSize = Integer.valueOf(Library.getProperty("org.zkoss.zss.spreadsheet.maxRenderedCellSize", "" + DEFAULT_MAX_RENDERED_CELL_SIZE));
		return _defMaxRenderedCellSize;
	}
	
//	private static Set<UserAction> getDefaultActiobDisabled() {
//		if (_defToolbarActiobDisabled == null) {
//			_defToolbarActiobDisabled = new HashSet<UserAction>();
//			HashMap<String, UserAction> toolbarActions = UserAction.getAll();
//			
//			String[] actions = Library.getProperty(TOOLBAR_DISABLED_ACTION, "").split(",");
//			for (String a : actions) {
//				String action = a.trim();
//				if (toolbarActions.containsKey(action)) {
//					_defToolbarActiobDisabled.add(toolbarActions.get(action));
//				}
//			}
//		}
//		return new HashSet<UserAction>(_defToolbarActiobDisabled); 
//	}
	
	/**
	 * Sets the max rendered cell size. When rendered cell size greater then this limit, 
	 * client side will prune extra cells (DOM Element). <br/>
	 * 
	 * @param maxRenderedCellSize
	 */
	public void setMaxRenderedCellSize(int maxRenderedCellSize) {
		if (_maxRenderedCellSize != maxRenderedCellSize) {
			_maxRenderedCellSize = maxRenderedCellSize;
			smartUpdate("maxRenderedCellSize", maxRenderedCellSize);
		}
	}
	
	/**
	 * Returns the max rendered cell size
	 * 
	 * @return int maxRenderedCellSize
	 */
	public int getMaxRenderedCellSize() {
		return _maxRenderedCellSize;
	}
	
	/**
	 * Returns whether client cache disabled or not 
	 * 
	 * @return
	 */
	public boolean isClientCacheDisabled() {
		return _clientCacheDisabled;
	}
	
	/**
	 * Sets to disable client cache. Default is false
	 * @param clientCacheDisabled
	 */
	public void setClientCacheDisabled(boolean clientCacheDisabled) {
		if (_clientCacheDisabled != clientCacheDisabled) {
			_clientCacheDisabled = clientCacheDisabled;
			smartUpdate("clientCacheDisabled", _clientCacheDisabled);
		}
	}

	/**
	 * Don't call this, the spreadsheet is not draggable.
	 * 
	 * @exception UnsupportedOperationException if this method is called.
	 */
	public void setDraggable(String draggable) {
		throw new UnsupportedOperationException("doesn't support to be draggable");
	}

	/**
	 * @return the book model of this spreadsheet.
	 * @deprecated since 3.0.0 , use {@link #getBook()}
	 */
	public SBook getXBook() {
		return getSBook();
	}
	
	/**
	 * Returns the book model of this Spreadsheet. If you call this method at
	 * first time and the book has not assigned by {@link #setSBook(SBook)}, this
	 * will create a new model depends on src;
	 * 
	 * @return the book model of this spreadsheet.
	 */
	public SBook getSBook() {
		if (_book == null) {
			if (_src == null) {
				return null;
			}
			try {
				SImporter importer = _importer;
				if (importer == null) {
					importer = SImporters.getImporter();
				}

				SBook book = null;
				{
					URL url = null;
					if (_src.startsWith("/")) {// try to load by application
						// context.
						WebApp wapp = Executions.getCurrent().getDesktop().getWebApp();
						String path = wapp.getRealPath(_src);
						if (path != null) {
							File file = new File(path);
//							if (file.isDirectory())
//								throw new IllegalArgumentException("Your input source is a directory, not a vaild file");
							if (file.exists())
								url = file.toURI().toURL();
						} else
							url = wapp.getResource(_src); 
					}
					if (url == null) {// try to load from class loader
						url = new ClassLocator().getResource(_src);
					}
					if (url == null) {// try to load from file
						File f = new File(_src);
						if (f.exists()) {
							url = f.toURI().toURL();
						}
					}

					if (url == null) {
						throw new UiException("resource for " + _src + " not found.");
					}

					String bookName = url.getFile();
					int i = bookName.lastIndexOf('/');
					if(i >=0){
						bookName = bookName.substring(i+1,bookName.length());
					}
					book = importer.imports(url,bookName);
				}
				initBook(book); //will set _book inside this method
			} catch (Exception ex) {
				throw UiException.Aide.wrap(ex);
			}
		}
		return _book;
	}

	/**
	 * Sets the book data model of this spread sheet.
	 * 
	 * @param book the book data model.
	 * @deprecated since 3.0.0 , use {@link #setBook(Book)}
	 */
	public void setXBook(SBook book) {
		setSBook(book);
	}
	
	/**
	 * Sets the book data model of this spread sheet.
	 * @param book the book data model.
	 */
	public void setSBook(SBook book) {
		if (!Objects.equals(book, _book)) {
			initBook0(book);
			invalidate();
		}
	}
	
	private void initBook(SBook book) {
		if (!Objects.equals(book, _book)) {
			initBook0(book);
		}
	}
	private Focus _selfEditorFocus;
	private static final String FRIEND_FOCUS_KEY = "zss.FirendFocus";
	
	private FriendFocusHelper getFriendFocusHelper(){
		FriendFocusHelper helper = null;
		if(_book!=null){
			_book.getBookSeries().getLock().writeLock().lock();
			try{
				helper = (FriendFocusHelper) _book.getAttribute(FRIEND_FOCUS_KEY);
				if(helper==null){
					_book.setAttribute(FRIEND_FOCUS_KEY, helper = new FriendFocusHelper());
				}
			}finally{
				_book.getBookSeries().getLock().writeLock().unlock();
			}
		}
		return helper;
	}
	
	private void deleteSelfEditorFocus() {
		if (_selectedSheet != null && getSBook().getSheetIndex(_selectedSheet) != -1  && _selfEditorFocus != null) {
			final SRange rng = SRanges.range(_selectedSheet);
			getFriendFocusHelper().removeFocus(_selfEditorFocus);
			rng.notifyCustomEvent(ON_MODEL_FRIEND_FOCUS_DELETE,_selfEditorFocus,true);
			_selfEditorFocus = null;
		}
	}
	private void moveSelfEditorFocus(String sheetId,int row, int column) {
		if (_selectedSheet != null) {
			if (_selfEditorFocus == null) {
				_selfEditorFocus = newSelfFocus(sheetId,row,column);
				getFriendFocusHelper().addFocus(_selfEditorFocus);
			}else{
				_selfEditorFocus.setSheetId(sheetId);
				_selfEditorFocus.setPosition(row, column);
			}
			final SRange rng = SRanges.range(_selectedSheet);
			rng.notifyCustomEvent(ON_MODEL_FRIEND_FOCUS_MOVE,_selfEditorFocus,true);
		}
		syncFriendFocus();
	}
	private EventListener<Event> _focusListener = null;

	private UserActionManagerCtrl _actionManagerCtrl;

	private boolean _showContextMenu;
	private void doMoveSelfFocus(CellEvent event){
		moveSelfEditorFocus(getSelectedSheetId(),event.getRow(),event.getColumn());
	}
	
	//ZSS-406 Spreadsheet doesn't be release when use application share scope
	private void releaseBook(){
		if (_book != null) {
			_book.getBookSeries().getLock().writeLock().lock();
			try{
				_book.removeEventListener(_modelEventListener);
				if(isBelowDesktopScope(_book) && _book instanceof EvaluationContributorContainer
						&& ((EvaluationContributorContainer)_book).getEvaluationContributor() instanceof ComponentEvaluationContributor){
					((EvaluationContributorContainer)_book).setEvaluationContributor(null);
				}
				//delete self focus that stores in the book.
				deleteSelfEditorFocus();
			}finally{
				_book.getBookSeries().getLock().writeLock().unlock();
			}
			_book = null;
		}
	}
	
	
	private boolean isBelowDesktopScope(SBook book){
		String scope = _book.getShareScope();
		return scope==null||"desktop".equals(scope);
	}
	
	private void initBook0(SBook book) {
		if (_book != null) {
			_book.getBookSeries().getLock().writeLock().lock();
			try{
				_book.removeEventListener(_modelEventListener);
				if(isBelowDesktopScope(_book) && _book instanceof EvaluationContributorContainer
						&& ((EvaluationContributorContainer)_book).getEvaluationContributor() instanceof ComponentEvaluationContributor){
					((EvaluationContributorContainer)_book).setEvaluationContributor(null);
				}
			
			}finally{
				_book.getBookSeries().getLock().writeLock().unlock();
			}
			if (_focusListener != null){
				removeEventListener(Events.ON_CELL_FOUCS, _focusListener);
				_focusListener = null;
			}
			deleteSelfEditorFocus();
		}
		
		 //Shall clean selected sheet before set new book (ZSS-75: set book null, cause NPE)
		cleanSelectedSheet();
		
		removeAttribute(MERGE_MATRIX_KEY);
		clearHeaderSizeHelper(true, true);
		//ZSS-343 ActiveRangeHelper caches sheet object and doesn't release after reload a book
		removeAttribute(ACTIVE_RANGE_KEY);
		
		_custColId = new SequenceId(-1, 2);
		_custRowId = new SequenceId(-1, 2);
		
		//clear undo history
		clearUndoableActionManager();
		
		_book = book;
		if (_book != null) {
			_book.getBookSeries().getLock().writeLock().lock();
			try{
				_book.addEventListener(_modelEventListener);
				if(isBelowDesktopScope(_book) && _book instanceof EvaluationContributorContainer 
						&& ((EvaluationContributorContainer)_book).getEvaluationContributor()==null){
					((EvaluationContributorContainer)_book).setEvaluationContributor(new ComponentEvaluationContributor(this));
				}
			}finally{
				_book.getBookSeries().getLock().writeLock().unlock();
			}
			
			//20130523, dennis, if share-scope is not empty, then should always sync the  focus, not only application and session
			//TODO use a configuration to config this.
			if (!Strings.isEmpty(_book.getShareScope())) { //have to sync focus
				this.addEventListener(Events.ON_CELL_FOUCS, _focusListener = new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						doMoveSelfFocus((CellEvent) event);
					}
				});
			}
			
		}
		_selfFocusId = null;//clean
		refreshToolbarDisabled();
		getUserActionManagerCtrl().doAfterLoadBook(getBook());
	}
	
	private Focus newSelfFocus(String sheetId, int row, int column) {
		//show id/uuid is useless for co-edit and ugly
		final String focusName = _userName == null ? ""/*+ (getId() == null ? getUuid() : getId()) */: _userName;
		_selfFocusId = _selfFocusId==null?getFriendFocusHelper().nextFocusId():_selfFocusId;
		Focus focus = new Focus(_selfFocusId, focusName, "#000", sheetId,row,column, this);
		return focus;
	}

	/**
	 * @deprecated since 3.0.0 , use {@link #getSelectedSheet()}
	 */
	public SSheet getSelectedXSheet() {
		return getSelectedSSheet();
	}
	/**
	 * Gets the selected sheet, the default selected sheet is first sheet.
	 * @return #{@link SSheet}
	 */	
	public SSheet getSelectedSSheet() {
		final SBook book = getSBook();
		if (book == null) {
			return null;
		}
		if (_selectedSheet == null) {
			if (book.getNumOfSheet() == 0)
				throw new UiException("sheet size of given book is zero");
			_selectedSheet = (SSheet) book.getSheet(0);
			afterSheetSelected();
		}
		return _selectedSheet;
	}
	
	private String getSelectedSheetId() {
		SSheet sheet = getSelectedSSheet();
		return sheet==null?null:sheet.getId();
	}

	/**
	 * Returns the src location of book model. This src is used by the specified
	 * importer to create the book data model of this spread sheet.
	 * 
	 * @return the src location
	 */
	public String getSrc() {
		return _src;
	}


	/**
	 * Sets the src location of the book data model to be imported into
	 * spreadsheet. A specified importer ({@link #getImporter}) will use this
	 * src to create the book data model.
	 * 
	 * @param src  the book src location
	 */
	public void setSrc(String src) {
		if (!Objects.equals(_src, src)) {
			_src = src;
			setBook(null);
			invalidate();
		}
	}
	
	/**
	 * Gets the importer that import the file in the specified src (
	 * {@link #getSrc}) to {@link XBook} data model. The default importer is
	 * {@link ExcelImporter}.
	 * 
	 * @return the importer
	 * @deprecated since 3.0.0 , use {@link #getImporter()}
	 */
	public SImporter getSImporter() {
		return _importer;
	}

	/**
	 * Sets the importer for import the book data model from a specified src.
	 * 
	 * @param importer the importer to import a spread sheet file from a document
	 * format (e.g. an Excel file) by the specified src (@link
	 * #setSrc(). The default importer is {@link ExcelImporter}.
	 * @deprecated since 3.0.0 , use {@link #setImporter(Importer)}
	 */
	public void setSImporter(SImporter importer) {
		if (!Objects.equals(importer, _importer)) {
			_importer = importer;
			setBook(null);
		}
	}

	/**
	 * Sets the selected sheet by a name
	 * @param name	the name of spreadsheet to be selected.
	 */
	public void setSelectedSheet(String name) {
		boolean update = setSelectedSheet0(name);
		
		//TODO: think if this is correct or not
		// the call of onSheetSelected must after invalidate,
		// because i must let invalidate clean lastcellblock first
		if(update){
			afterSheetSelected();
			invalidate();
		}
	}
	
	/**
	 * @return true if selected another sheet
	 */
	private boolean setSelectedSheet0(String name) {
		final SBook book = getSBook();
		if (book == null) {
			return false;
		}
		boolean update = false;
		//Note. check whether if the sheet has remove or not
		if (_selectedSheet != null && book.getSheetIndex(_selectedSheet) == -1) {
			cleanSelectedSheet();
			//_selectedSheet become null after clean
			update = true;
		}

		if (_selectedSheet == null || !_selectedSheet.getSheetName().equals(name)) {
			SSheet sheet = book.getSheetByName(name);
			if (sheet == null) {
				throw new UiException("No such sheet : " + name);
			}
			cleanSelectedSheet();
			
			_selectedSheet = sheet;
			update = true;
		}
		return update;
	}
	
	/*
	 * DefaultUserActionManagerCtrl will update highlight area when selecting a sheet.
	 */
	private void setSelectedSheetDirectly(String name, boolean cacheInClient, int row, int col, 
			int left, int top, int right, int bottom
			/*, int highlightLeft, int highlightTop, int highlightRight, int highlightBottom,
			int rowfreeze, int colfreeze*/) {
		boolean update = setSelectedSheet0(name);
		if (row >= 0 && col >= 0) {
			this.setCellFocusDirectly(new CellRef(row, col));
		} else {
			this.setCellFocusDirectly(new CellRef(0, 0));
		}
		if (top >= 0 && right >= 0 && bottom >= 0 && left >=0) {
			this.setSelectionDirectly(new AreaRef(top, left, bottom, right));
		} else {
			this.setSelectionDirectly(new AreaRef(0, 0, 0, 0));
		}
		if(update){
			afterSheetSelected();
		}
		
		updateSheetAttributes(cacheInClient/*, rowfreeze, colfreeze*/);
	}
	
	private void updateSheetAttributes(boolean cacheInClient/*, int rowfreeze, int colfreeze*/) {
		
		SSheet sheet = _selectedSheet;
		
		String css = getDynamicMediaURI(this, _cssVersion++, "ss_" + this.getUuid() + "_" + getSelectedSheetId(), "css");
		smartUpdate("scss", css);
		if (!cacheInClient)	{
			
			smartUpdate("rowFreeze", getSelectedSheetRowfreeze());
			smartUpdate("columnFreeze", getSelectedSheetColumnfreeze());
			
			//handle AutoFilter
			Map afmap = convertAutoFilterToJSON(sheet.getAutoFilter());
			if (afmap != null) {
				smartUpdate("_autoFilter", afmap);
			} else {
				smartUpdate("_autoFilter", (String) null);
			}
			
			smartUpdate("rowHeight", getRowheight());
			smartUpdate("columnWidth", getColumnwidth());
			
			smartUpdate("displayGridlines", !_hideGridlines);
			refreshAllowedOptions();
			updateUnlockInfo();
			smartUpdate("protect", _protectSheet);
			
			// generate customized row & column information
			HeaderPositionHelper colHelper = getColumnPositionHelper(sheet);
			HeaderPositionHelper rowHelper = getRowPositionHelper(sheet);
			smartUpdate("csc", getSizeHelperStr(colHelper));
			smartUpdate("csr", getSizeHelperStr(rowHelper));
			
			// generate merge range information
			MergeMatrixHelper mmhelper = getMergeMatrixHelper(sheet);
			Iterator iter = mmhelper.getRanges().iterator();
			StringBuffer merr = new StringBuffer();
			while (iter.hasNext()) {
				MergedRect block = (MergedRect) iter.next();
				int left = block.getColumn();
				int top = block.getRow();
				int right = block.getLastColumn();
				int bottom = block.getLastRow();
				int id = block.getId();
				merr.append(left).append(",").append(top).append(",").append(right).append(",").append(bottom).append(",").append(id);
				if (iter.hasNext()) {
					merr.append(";");
				}
			}
			smartUpdate("mergeRange", merr.toString());
			
			// ZSS-423: create multiple "active range" data for every panel
			final SpreadsheetCtrl spreadsheetCtrl = ((SpreadsheetCtrl) this.getExtraCtrl());
			JSONObject activeRange = createActiveRange(spreadsheetCtrl, sheet, getInitColumnSize(), getInitRowSize());
			smartUpdate("activeRange", activeRange);
			
			//handle Validation, must after render("activeRange"
			List<Map<String, Object>> dvs = getDataValidationHandler().loadDataValidtionJASON(getSelectedSheet());
			if (dvs != null) {
				smartUpdate("dataValidations", dvs);
			} else {
				smartUpdate("dataValidations", (String) null);
			}
		}
		
		smartUpdate("sheetId", getSelectedSheetId());
	}

	/**
	 * Returns the maximum visible number of rows of this spreadsheet. You can assign
	 * new number by calling {@link #setMaxrows(int)}.
	 * 
	 * @return the maximum visible number of rows.
	 * @deprecated since 3.0.0, use {@code #getMaxVisibleRows()} 
	 */
	public int getMaxrows() {
		return getMaxVisibleRows();
	}

	/**
	 * Sets the maximum visible number of rows of this spreadsheet. For example, if you set
	 * this parameter to 40, it will allow showing only row 0 to 39. The minimal value of max number of rows
	 * must large than 0; <br/>
	 * Default : 20.
	 * 
	 * @param maxrows  the maximum visible number of rows
	 * @deprecated since 3.0.0, use {@code #setMaxVisibleRows(int)} 
	 */
	public void setMaxrows(int maxrows) {
		setMaxVisibleRows(maxrows);
	}
	
	public void setPreloadRowSize(int size) {
		if (_preloadRowSize != size) {
			_preloadRowSize = size <= 0 ? 0 : size;
			smartUpdate("preloadRowSize", _preloadRowSize);
		}
	}
	
	public int getPreloadRowSize() {
		return _preloadRowSize;
	}
	
	/**
	 * Returns the number of rows rendered when Spreadsheet first render
	 * @return int
	 */
	private int getInitRowSize() {
		int rowSize = SpreadsheetCtrl.DEFAULT_LOAD_ROW_SIZE;
		int preloadRowSize = getPreloadRowSize();
		if (preloadRowSize == -1) {
			rowSize = Math.min(rowSize, getMaxrows() - 1);
		} else {
			rowSize = Math.min(preloadRowSize - 1, getMaxrows() - 1);
		}
		return rowSize;
	}
	
	/**
	 * Returns the maximum visible number of columns of this spreadsheet. You can assign
	 * new numbers by calling {@link #setMaxcolumns(int)}.
	 * 
	 * @return the maximum visible number of columns
	 * @deprecated since 3.0.0, use {@code #getMaxVisibleColumns()} 
	 */
	public int getMaxcolumns() {
		return getMaxVisibleColumns();
	}

	/**
	 * Sets the maximum visible number of columns of this spreadsheet. For example, if you
	 * set this parameter to 40, it will allow showing only column 0 to column 39. the minimal value of
	 * max number of columns must large than 0;
	 * 
	 * @param maxcols  the maximum visible number of columns
	 * @deprecated since 3.0.0, use {@code #setMaxVisibleColumns(int)} 
	 */
	public void setMaxcolumns(int maxcols) {
		setMaxVisibleColumns(maxcols);
	}
	
	public void setPreloadColumnSize(int size) {
		if (_preloadColumnSize != size) {
			_preloadColumnSize = size < 0 ? 0 : size;
			smartUpdate("preloadColSize", _preloadColumnSize);
		}
	}
	
	public int getPreloadColumnSize() {
		return _preloadColumnSize;
	}
	
	/**
	 * Returns the number of columns rendered when Spreadsheet first render
	 * @return int
	 */
	private int getInitColumnSize() {
		int colSize = SpreadsheetCtrl.DEFAULT_LOAD_COLUMN_SIZE;
		int preloadColSize = getPreloadColumnSize();
		if (preloadColSize == -1) {
			colSize = Math.min(colSize, getMaxcolumns() - 1);
		} else {
			colSize = Math.min(preloadColSize - 1, getMaxcolumns() - 1);
		}
		return colSize;
	}
	
	/**
	 * Returns the index of row freeze of this of spreadsheet or selected sheet.
	 * @deprecated since 3.0.0, use {@link Sheet#getRowFreeze()} instead.
	 */
	public int getRowfreeze() {
		return -1;
	}
	
	/**
	 * Sets the index of row freeze of this spreadsheet.
	 * @deprecated since 3.0.0, use {@link Range#setFreezePanel(int, int)} instead.
	 */
	public void setRowfreeze(int rowfreeze) {
		
	}

	/**
	 * Returns the index of column freeze of this of spreadsheet or selected sheet.
	 * @deprecated since 3.0.0, use {@link Sheet#getColumnFreeze()} instead.
	 */
	public int getColumnfreeze() {
		return -1;
	}
	
	/**
	 * Sets the index of column freeze of this spreadsheet.
	 * @deprecated since 3.0.0, use {@link Range#setFreezePanel(int, int)} instead.
	 */
	public void setColumnfreeze(int colfreeze) {
	}
	
	final private int getSelectedSheetRowfreeze(){
		return getFreezeInfoLoader().getRowFreeze(getSelectedSheet());
	}
	
	final private int getSelectedSheetColumnfreeze(){
		return getFreezeInfoLoader().getColumnFreeze(getSelectedSheet());
	}

	/**
	 * Returns true if hide row head of this spread sheet; default to false.
	 * @return true if hide row head of this spread sheet; default to false.
	 */
	public boolean isHiderowhead() {
		return _hideRowhead;
	}

	/**
	 * Sets true to hide the row head of this spread sheet.
	 * @param hide true to hide the row head of this spread sheet.
	 */
	public void setHiderowhead(boolean hide) {
		// TODO
		if (_hideRowhead != hide) {
			_hideRowhead = hide;
			invalidate();
			/**
			 * rename. hiderowhead -> rowHeadHidden, not implement yet
			 */
			//smartUpdate("rowHeadHidden", _hideRowhead);
		}
	}

	/**
	 * Returns true if hide column head of this spread sheet; default to false.
	 * @return true if hide column head of this spread sheet; default to false.
	 */
	public boolean isHidecolumnhead() {
		return _hideColhead;
	}

	/**
	 * Sets true to hide the column head of this spread sheet.
	 * @param hide true to hide the row head of this spread sheet.
	 */
	public void setHidecolumnhead(boolean hide) {
		// TODO
		if (_hideColhead != hide) {
			_hideColhead = hide;
			invalidate();
			/**
			 * rename. hidecolhead ->columnHeadHidden, not implement yet
			 */
			// smartUpdate("z.hidecolhead", b);
			// smartUpdate("columnHeadHidden", _hideColhead);
		}
	}

	/**
	 * Sets the customized titles of column header.
	 * 
	 * @param titles a map for customized column titles, the key of column must be Integer object.
	 */
	public void setColumntitles(Map titles) {
		if (!Objects.equals(titles, _columnTitles)) {
			_columnTitles = titles;
			invalidate();
		}
	}

	/**
	 * Get the column titles Map object, modification of return object doesn't
	 * cause any update.
	 * 
	 * @return Map object of customized column names
	 */
	public Map getColumntitles() {
		return _columnTitles;
	}

	/**
	 * Sets the customized titles which split by ','. For example:
	 * "name,title,age" or "name,,age" , an empty string means use default
	 * column title. This method will split the input string to a Map with
	 * sequence index from 0, then call {@link #setColumntitles(Map)}<br/>
	 * 
	 * <p/>
	 * Note: this method will always invoke invalidate()
	 * 
	 * @param titles  the column titles
	 */
	public void setColumntitles(String titles) {
		String[] names = titles.split(",");
		Map map = new HashMap();
		for (int i = 0; i < names.length; i++) {
			if (names[i].length() > 0)
				map.put(Integer.valueOf(i), names[i]);
		}
		if (map.size() > 0)
			setColumntitles(map);

	}

	/**
	 * Sets the customized titles of row header.
	 * 
	 * @param titles map for customized column titles, the key of column must be Integer object.
	 */
	public void setRowtitles(Map titles) {
		if (!Objects.equals(titles, _rowTitles)) {
			_rowTitles = titles;
			invalidate();
		}
	}

	/**
	 * Get the row titles Map object, modification of the return object doesn't
	 * cause any update.
	 * 
	 * @return Map object of customized row names
	 */
	public Map getRowtitles() {
		return _rowTitles;
	}

	/**
	 * Sets the customized titles which split by ','. For example:
	 * "name,title,age" or "name,,age" , an empty string means use default row
	 * title. This method will split the input string to a Map with sequence
	 * index from 0, then call {@link #setRowtitles(Map)}<br/>
	 * 
	 * <p/>
	 * Note: this method will always invoke invalidate()
	 * 
	 * @param titles the row titles
	 */
	public void setRowtitles(String titles) {
		String[] names = titles.split(",");
		Map map = new HashMap();
		for (int i = 0; i < names.length; i++) {
			if (names[i].length() > 0)
				map.put(Integer.valueOf(i), names[i]);
		}
		if (map.size() > 0)
			setRowtitles(map);
	}

	/**
	 * Gets the default row height of the selected sheet
	 * @return default value depends on selected sheet
	 */
	public int getRowheight() {
		SSheet sheet = getSelectedSSheet();
		int rowHeight = sheet != null ? sheet.getDefaultRowHeight() : -1;

		return (rowHeight <= 0) ? _defaultRowHeight : rowHeight;
	}

	
	/**
	 * Sets the default row height of the selected sheet
	 * @param rowHeight the row height
	 */
	public void setRowheight(int rowHeight) {
		SSheet sheet = getSelectedSSheet();
		
		int dh = sheet.getDefaultRowHeight();

		if (dh != rowHeight) {
			sheet.setDefaultRowHeight(rowHeight);
			invalidate();
			/**
			 * rename rowh -> rowHeight, not implement yet
			 */
			//smartUpdate("rowHeight", rowHeight);
		}
	}
	
	/**
	 * Gets the default column width of the selected sheet
	 * @return default value depends on selected sheet
	 */
	public int getColumnwidth() {
		final SSheet sheet = getSelectedSSheet();
		return sheet.getDefaultColumnWidth();//XUtils.getDefaultColumnWidthInPx(sheet);
	}

	/**
	 * Sets the default column width of the selected sheet
	 * @param columnWidth the default column width
	 */
	public void setColumnwidth(int columnWidth) {
		final SSheet sheet = getSelectedSSheet();
		int dw = sheet.getDefaultColumnWidth();
		if (dw != columnWidth) {
			sheet.setDefaultColumnWidth(columnWidth);
			
			invalidate();
			/**
			 * rename colw -> columnWidth , not implement yet
			 */
			//smartUpdate("columnWidth", columnWidth);
		}
	}
	
//	private int getDefaultCharWidth() {
//		final XSheet sheet = getSelectedXSheet();
//		return XUtils.getDefaultCharWidth(sheet);
//	}

	/**
	 * Gets the left head panel width
	 * @return default value is 36
	 */
	public int getLeftheadwidth() {
		return _leftheadWidth;
	}
	
	/**
	 * rename setLeftheadwidth -> setLeftheadWidth
	 */
	/**
	 * Sets the left head panel width, must large then 0.
	 * @param leftWidth leaf header width
	 */
	public void setLeftheadwidth(int leftWidth) {
		if (_leftheadWidth != leftWidth) {
			_leftheadWidth = leftWidth;
			invalidate();
			/**
			 * leftw -> leftPanelWidth, not implement yet
			 */
			// smartUpdate("z.leftw", leftWidth);
			//smartUpdate("leftPanelWidth", leftWidth);
		}
	}

	/**
	 * Gets the top head panel height
	 * @return default value is 20
	 */
	public int getTopheadheight() {
		return _topheadHeight;
	}

	/**
	 * Sets the top head panel height, must large then 0.
	 * @param topHeight top header height
	 */
	public void setTopheadheight(int topHeight) {
		if (_topheadHeight != topHeight) {
			_topheadHeight = topHeight;
			invalidate();
			/**
			 * toph ->topPanelHeight -> , not implement yet
			 */
			//smartUpdate("topPanelHeight", topHeight);
		}
	}
	
	/**
	 * Sets whether show toolbar or not
	 * 
	 * Default: false
	 * @param showToolbar true to show toolbar
	 */
	public void setShowToolbar(boolean showToolbar) {
		if (_showToolbar != showToolbar) {
			_showToolbar = showToolbar;
			smartUpdate("showToolbar", _showToolbar);
		}
	}
	
	/**
	 * Returns whether shows toolbar
	 * @return boolean
	 */
	public boolean isShowToolbar() {
		return _showToolbar;
	}
	
	/**
	 * Sets whether show formula bar or not
	 * @param showFormulabar true if want to show formula bar
	 */
	public void setShowFormulabar(boolean showFormulabar) {
		if (_showFormulabar != showFormulabar) {
			_showFormulabar = showFormulabar;
			smartUpdate("showFormulabar", _showFormulabar);
		}
	}
	
	/**
	 * Returns whether show formula bar
	 */
	public boolean isShowFormulabar() {
		return _showFormulabar;
	}
	
	/**
	 * Sets whether show sheetbar or not
	 * @param showSheetbar true if want to show sheet tab panel
	 */
	public void setShowSheetbar(boolean showSheetbar) {
		if (_showSheetbar != showSheetbar) {
			_showSheetbar = showSheetbar;
			smartUpdate("showSheetbar", _showSheetbar);
		}
	}
	
	/**
	 * Returns whether show sheetbar
	 */
	public boolean isShowSheetbar() {
		return _showSheetbar;
	}
	
	/**
	 * Sets whether show ContextMenu or not
	 * @param showContextMenu
	 */
	public void setShowContextMenu(boolean showContextMenu) {
		if (_showContextMenu != showContextMenu) {
			_showContextMenu = showContextMenu;
			smartUpdate("showContextMenu", _showContextMenu);
		}
	}
	
	/**
	 * Returns whether show ContextMenu
	 * @return
	 */
	public boolean isShowContextMenu() {
		return _showContextMenu;
	}
	
	private Map convertAutoFilterToJSON(SAutoFilter af) {
		if (af != null) {
			final CellRegion addr = af.getRegion();
			if (addr == null) {
				return null;
			}
			final Map addrmap = new HashMap();
			final int left = addr.getColumn();
			final int right = addr.getLastColumn();
			final int top = addr.getRow();
			final SSheet sheet = this.getSelectedSSheet();
			addrmap.put("left", left);
			addrmap.put("top", top);
			addrmap.put("right", right);
			addrmap.put("bottom", addr.getLastRow());
			
			int offcol = -1;
			final Collection<NFilterColumn> fcs = af.getFilterColumns();
			final List<Map> fcsary = fcs != null ? new ArrayList<Map>(fcs.size()) : null;
			if (fcsary != null) {
				for(int col = left; col <= right; ++col) {
					final NFilterColumn fc = af.getFilterColumn(col - left,false);
					final List<String> filters = fc != null ? fc.getFilters() : null;
					final boolean on = fc != null ? fc.isShowButton() : true;
					Map fcmap = new HashMap();
					fcmap.put("col", Integer.valueOf(col));
					fcmap.put("filter", filters);
					fcmap.put("on", on);
					int field = col - left + 1;
//					if (offcol >= 0 && on) { //pre button not shown and I am shown, the field number might be different!
//						field = offcol - left + 1;
//					}
					fcmap.put("field", field);
//					if (!on) {
//						if (offcol < 0) { //first button off column
//							offcol = col;
//						}
//					} else {
//						offcol = -1;
//					}
					fcsary.add(fcmap);
				}
			}
			
			final Map afmap = new HashMap();
			afmap.put("range", addrmap);
			afmap.put("filterColumns", fcsary);
			return afmap;
		}
		return null;
	}

	

	//ZSS-13: Support Open hyperlink in a separate browser tab window
	private boolean getLinkToNewTab() {
		final String linkToNewTab = Library.getProperty("org.zkoss.zss.ui.Spreadsheet.linkToNewTab", "true");
		return Boolean.valueOf(linkToNewTab);
	}
	
	/**
	 * Returns each sheet's name and sheet uuid
	 * @return
	 */
	private List<LinkedHashMap<String, String>> getSheetLabels() {
		int len = _book.getNumOfSheet();
		List<LinkedHashMap<String, String>> ary = new ArrayList<LinkedHashMap<String, String>>(len);

		for (int i = 0; i < len; i++) {
			//key: sheet names, value: sheet uuid
			LinkedHashMap<String, String> sheetLabels = new LinkedHashMap<String, String>();
			
			SSheet sheet = _book.getSheet(i);
			sheetLabels.put("id", sheet.getId());
			sheetLabels.put("name", sheet.getSheetName());
			if (sheet == _selectedSheet)
				sheetLabels.put("sel", "t");//stand for true, use for set selected tab only 
				
			ary.add(sheetLabels);
		}
		return ary.size() == 0 ? null : ary;
	}
	
	protected void renderProperties(ContentRenderer renderer) throws IOException {
		SBook book = getSBook();
		if(book==null){
			renderProperties0(renderer);
		}else{
			ReadWriteLock lock = book.getBookSeries().getLock();
			lock.writeLock().lock();//have to use write lock because of formula evaluation is not thread safe
			try{
				renderProperties0(renderer);
			}finally{
				lock.writeLock().unlock();
			}
		}
	}
	protected void renderProperties0(ContentRenderer renderer) throws IOException {
		super.renderProperties(renderer);
		//I18N labels, must set first
		//TODO review this part 
//		Map<String, String> labels = getLabels();
//		if (labels != null) {
//			renderer.render("labels", labels);
//		}
		
		renderer.render("colorPickerExUsed", isColorPickerExUsed()); //must before rendering showToolbar for the property used in creating toolbar buttons
		if (_showToolbar || _showContextMenu || _showSheetbar) { // ZSS-252, _showContextMenu and _showSheetbar need actionDsiabled information in client-side.
			//20130507,Dennis,add commnet check, no actionDisabled json will cause client error when show context menu.
//			if (_actionDisabled.size() > 0) {
			renderer.render("actionDisabled",
					convertToDisabledActionJSON(getUserActionManagerCtrl()
							.getSupportedUserAction(getSelectedSheet())));
//			}
			renderer.render("showToolbar", _showToolbar);
		}
			
		renderer.render("showFormulabar", _showFormulabar);
		SSheet sheet = this.getSelectedSSheet();
		if (sheet == null) {
			return;
		}
		
		if (_showContextMenu) {
			renderer.render("showContextMenu", _showContextMenu);	
		}
		
		if (_clientCacheDisabled) //default: use client cache
			renderer.render("clientCacheDisabled", _clientCacheDisabled);
		
		if (_maxRenderedCellSize != DEFAULT_MAX_RENDERED_CELL_SIZE)
			renderer.render("maxRenderedCellSize", _maxRenderedCellSize);
		
		//Note: sheetLabels (sheet name, sheet uuid) must before showSheetTabpanel
		List<LinkedHashMap<String, String>> sheetLabels = getSheetLabels();
		if (sheetLabels != null) {
			renderer.render("sheetLabels", sheetLabels);
		}
		if (_showSheetbar)
			renderer.render("showSheetbar", _showSheetbar);
		
		//handle link to new browser tab window; default to link to new tab
		if (!getLinkToNewTab()) {
			renderer.render("_linkToNewTab", false);
		}
		
		//handle AutoFilter
		Map afmap = convertAutoFilterToJSON(sheet.getAutoFilter());
		if (afmap != null) {
			renderer.render("autoFilter", afmap);
		} else {
			renderer.render("autoFilter", (String) null);
		}

		int rowHeight = getRowheight();
		if (rowHeight != DEFAULT_ROW_HEIGHT) {
			renderer.render("rowHeight", getRowheight());
		}
		renderer.render("columnWidth", getColumnwidth());
		
		if (_hideGridlines) {
			renderer.render("displayGridlines", !_hideGridlines);
		}
		if (_protectSheet) {
			final SheetProtection sheetProtection = Ranges.range(getSelectedSheet()).getSheetProtection();
			renderer.render("allowSelectLockedCells", sheetProtection.isSelectLockedCellsAllowed());
			renderer.render("allowSelectUnlockedCells", sheetProtection.isSelectUnlockedCellsAllowed());
			renderer.render("allowFormatCells", sheetProtection.isFormatCellsAllowed());
			renderer.render("allowFormatColumns", sheetProtection.isFormatColumnsAllowed());
			renderer.render("allowFormatRows", sheetProtection.isFormatRowsAllowed());
			renderer.render("allowAutoFilter", sheetProtection.isAutoFilterAllowed());
			renderer.render("objectEditable", sheetProtection.isObjectsEditable());
			if (!sheetProtection.isSelectLockedCellsAllowed() &&
					sheetProtection.isSelectUnlockedCellsAllowed()) {
				JSONObject unlockInfo = createUnlockInfo(getSelectedSheet());
				renderer.render("unlockInfo", unlockInfo);
			}
			renderer.render("protect", _protectSheet);
		}
		
		renderer.render("topPanelHeight", isHidecolumnhead() ? 1 : this.getTopheadheight());
		renderer.render("leftPanelWidth", isHiderowhead() ? 1 : this.getLeftheadwidth());

		if (_cellpadding != DEFAULT_CELL_PADDING)
			renderer.render("cellPadding", _cellpadding);
		
		String sheetId = getSelectedSheetId();
		String css = getDynamicMediaURI(this, _cssVersion++, "ss_" + this.getUuid() + "_" + sheetId, "css");
		renderer.render("loadcss", new JavaScriptValue("zk.loadCSS('" + css + "', '" + this.getUuid() + "-sheet')"));
		renderer.render("scss", css);

		int maxRows = getMaxrows();
		if (maxRows != DEFAULT_MAX_ROWS) {
			renderer.render("maxRows", maxRows);
		}
		int maxCols = getMaxcolumns();
		if (maxCols != DEFAULT_MAX_COLUMNS) {
			renderer.render("maxColumns", maxCols);
		}
		int rowFreeze = getSelectedSheetRowfreeze();
		if (rowFreeze > -1) {
			renderer.render("rowFreeze", rowFreeze);
		}
		int colFreeze = getSelectedSheetColumnfreeze();
		if (colFreeze > -1) {
			renderer.render("columnFreeze", colFreeze);
		}
		
		renderer.render("sheetId", getSelectedSheetId());
		renderer.render("focusRect", getRectStr(_focusArea));
		renderer.render("selectionRect", getRectStr(_selectionArea));
		if (_highlightArea != null) {
			renderer.render("highLightRect", getRectStr(_highlightArea));
		}

		// generate customized row & column information
		HeaderPositionHelper colHelper = getColumnPositionHelper(sheet);
		HeaderPositionHelper rowHelper = getRowPositionHelper(sheet);
		renderer.render("csc", getSizeHelperStr(colHelper));
		renderer.render("csr", getSizeHelperStr(rowHelper));

		// generate merge range information

		MergeMatrixHelper mmhelper = getMergeMatrixHelper(sheet);
		Iterator iter = mmhelper.getRanges().iterator();
		StringBuffer merr = new StringBuffer();
		while (iter.hasNext()) {
			MergedRect block = (MergedRect) iter.next();
			int left = block.getColumn();
			int top = block.getRow();
			int right = block.getLastColumn();
			int bottom = block.getLastRow();
			int id = block.getId();
			merr.append(left).append(",").append(top).append(",").append(right).append(",").append(bottom).append(",").append(id);
			if (iter.hasNext()) {
				merr.append(";");
			}
		}
		renderer.render("mergeRange", merr.toString());
		/**
		 * Add attr for UI renderer
		 */
		final SpreadsheetCtrl spreadsheetCtrl = ((SpreadsheetCtrl) this.getExtraCtrl());
		
		int initColSize = getInitColumnSize();
		int initRowSize = getInitRowSize();
		
		JSONObject activeRange = createActiveRange(spreadsheetCtrl, sheet, initColSize, initRowSize); // ZSS-423: create multiple "active range" data for every panel
		renderer.render("activeRange", activeRange);
		
		renderer.render("preloadRowSize", getPreloadRowSize());
		renderer.render("preloadColumnSize", getPreloadColumnSize());
		
		renderer.render("initRowSize", initRowSize);
		renderer.render("initColumnSize", initColSize);
		
		renderer.render("columnHeadHidden", _hideColhead);
		renderer.render("rowHeadHidden", _hideRowhead);
		
		//handle Validation, must after render("activeRange" ...)
		List<Map<String, Object>> dvs = getDataValidationHandler().loadDataValidtionJASON(getSelectedSheet());
		if (dvs != null) {
			renderer.render("dataValidations", dvs);
		} else {
			renderer.render("dataValidations", (String) null);
		}

	}
	
	private Boolean isColorPickerExUsed() {
		
		Object value = getAttribute(COLOR_PICKER_EX_USED_KEY, true);
		if(value == null){
			value = Library.getProperty(COLOR_PICKER_EX_USED_KEY, "false");
		}
		if(value != null){
			return Boolean.parseBoolean(value.toString());
		}
		return false;
	}
	
	/**
	 * create active range from coordination (0, 0) to specific size.
	 */
	private JSONObject createActiveRange(SpreadsheetCtrl spreadsheetCtrl, SSheet sheet, int columnCount, int rowCount) {

		// ZSS-423: create multiple "active range" data for every panel
		JSONObject activeRange = spreadsheetCtrl.getRangeAttrs(sheet, SpreadsheetCtrl.Header.BOTH,
				SpreadsheetCtrl.CellAttribute.ALL, 0, 0, columnCount, rowCount);
		
		int rowFreeze = getFreezeInfoLoader().getRowFreeze(sheet);
		int colFreeze = getFreezeInfoLoader().getColumnFreeze(sheet);
		
		if(rowFreeze >= 0) {
			activeRange.put("topFrozen", spreadsheetCtrl.getRangeAttrs(sheet, SpreadsheetCtrl.Header.BOTH,
					SpreadsheetCtrl.CellAttribute.ALL, 0, 0, columnCount, rowFreeze));
		}
		
		if(colFreeze >= 0) {
			activeRange.put("leftFrozen", spreadsheetCtrl.getRangeAttrs(sheet, SpreadsheetCtrl.Header.BOTH,
					SpreadsheetCtrl.CellAttribute.ALL, 0, 0, colFreeze, rowCount));
		}
		
		if(rowFreeze >= 0 && colFreeze >= 0) {
			activeRange.put("cornerFrozen", spreadsheetCtrl.getRangeAttrs(sheet, SpreadsheetCtrl.Header.BOTH,
					SpreadsheetCtrl.CellAttribute.ALL, 0, 0, colFreeze, rowFreeze));
		}

		return activeRange;
	}

	/**
	 * Get Column title of given index, it returns column name depends on
	 * following condition<br/>
	 * 1.if there is a custom column title assign on this spreadsheet on index,
	 * then return custom value <br/>
	 * 2.if there is a custom column title assign on selected sheet on index,
	 * the return custom value <br/>
	 * 3.return default column String, for example index 0 become "A", index 9
	 * become "J"
	 * 
	 * @return column name
	 */
	public String getColumntitle(int index) {

		if (XUtils.isTitleIndexMode(this)) {
			return Integer.toString(index);
		}
		String cname;
		if (_columnTitles != null
				&& (cname = (String) _columnTitles.get(Integer.valueOf(index))) != null) {
			return cname;
		}
		return org.zkoss.poi.ss.util.CellReference.convertNumToColString(index);
	}

	/**
	 * Get Row title of given index, it returns row name depends on following
	 * condition<br/>
	 * 1.if there is a custom row title assign on this spreadsheet on index,
	 * then return custom value <br/>
	 * 2.if there is a custom row title assign on selected sheet on index, the
	 * return custom value <br/>
	 * 3.return default row index+1 String, for example index 0 become "1",
	 * index 9 become "10"
	 * 
	 * @param index row index
	 * @return row name
	 */
	public String getRowtitle(int index) {
		String rname = null;

		if (XUtils.isTitleIndexMode(this)) {
			return Integer.toString(index);
		}

		if (_rowTitles != null && (rname = (String) _rowTitles.get(Integer.valueOf(index))) != null) {
			return rname;
		}

		return ""+(index+1);
	}

	/**
	 * Return current selection rectangle only if onCellSelection event listener is registered. 
	 * The returned value is a clone copy of current selection status. 
	 * Default Value:(0,0,0,0)
	 * @return current selection
	 */
	public AreaRef getSelection() {
		return (AreaRef) _selectionArea.cloneSelf();
	}

	/**
	 * Sets the selection rectangle. In general, if you set a selection, you must
	 * also set the focus by {@link #setCellFocus(CellRef)};. And, if you want
	 * to get the focus back to spreadsheet, call {@link #focus()} after set
	 * selection.
	 * 
	 * @param sel the selection rect
	 */
	public void setSelection(AreaRef sel) {
		if (!Objects.equals(_selectionArea, sel)) {
			;
			if (sel.getColumn() < 0 || sel.getRow() < 0
					|| sel.getLastColumn() > _book.getMaxColumnIndex()
					|| sel.getLastRow() > _book.getMaxRowIndex()
					|| sel.getColumn() > sel.getLastColumn()
					|| sel.getRow() > sel.getLastRow()) {
				throw new UiException("illegal selection : " + sel.toString());
			}
			setSelectionDirectly(sel);
		}
	}
	
	private void setSelectionDirectly(AreaRef sel) {
		_selectionArea.setArea(sel.getRow(), sel.getColumn(), sel.getLastRow(), sel.getLastColumn());

		HashMap args = new HashMap();
		args.put("type", "move");
		args.put("left", sel.getColumn());
		args.put("top", sel.getRow());
		args.put("right", sel.getLastColumn());
		args.put("bottom", sel.getLastRow());
		
		response("selection" + this.getUuid(), new AuSelection(this, args));
	}

	/**
	 * Return current highlight rectangle. the returned value is a clone copy of
	 * current highlight status. Default Value: null
	 * 
	 * @return current highlight
	 */
	public AreaRef getHighlight() {
		if (_highlightArea == null)
			return null;
		return (AreaRef) _highlightArea.cloneSelf();
	}

	/**
	 * Sets the highlight rectangle or sets a null value to hide it.
	 * 
	 * @param highlight the highlight rect
	 */
	public void setHighlight(AreaRef highlight) {
		if (!Objects.equals(_highlightArea, highlight)) {
			setHighlightDirectly(highlight);
		}
	}
	
	private void setHighlightDirectly(AreaRef highlight) {
		HashMap args = new HashMap();
		
		if (highlight == null) {
			_highlightArea = null;
			args.put("type", "hide");
		} else {
			final int left = Math.max(highlight.getColumn(), 0);
			final int right = Math.min(highlight.getLastColumn(), this.getMaxcolumns()-1);
			final int top = Math.max(highlight.getRow(), 0);
			final int bottom = Math.min(highlight.getLastRow(), this.getMaxrows()-1);
			if (left > right || top > bottom) {
				_highlightArea = null;
				args.put("type", "hide");
			} else {
				_highlightArea = new AreaRef(top, left, bottom, right);
				args.put("type", "show");
				args.put("left", left);
				args.put("top", top);
				args.put("right", right);
				args.put("bottom", bottom);
			}
		}
		response("selectionHighlight", new AuHighlight(this, args));
	}
	
	/**
	 * Sets whether display the gridlines.
	 * @param show true to show the gridlines.
	 */
	private void setDisplayGridlines(boolean show) {
		if (_hideGridlines == show) {
			_hideGridlines = !show;
			smartUpdate("displayGridlines", show);
		}
	}
	
	/**
	 * Update autofilter buttons.
	 * @param af the current AutoFilter.
	 */
	private void updateAutoFilter(SSheet sheet) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		if (this.isInvalidated())
			return;// since it is invalidate, we don't need to do anymore
		
		smartUpdate("autoFilter", convertAutoFilterToJSON(sheet.getAutoFilter()));
	}

    /**
     * Sets the sheet protection
     * @param boolean protect
     */
	private void setProtectSheet(boolean protect) {
		if (_protectSheet != protect) {
			_protectSheet = protect;
			if (protect) {
				refreshAllowedOptions();
				updateUnlockInfo();
			}
			smartUpdate("protect", protect);
			
			refreshToolbarDisabled();
		}
	}

	/**
	 * Return current cell(row,column) focus position. you can get the row by
	 * {@link CellRef#getRow()}, get the column by {@link CellRef#getColumn()}
	 * . The returned value is a copy of current focus status. Default
	 * Value:(0,0)
	 * 
	 * @return current focus
	 */
	public CellRef getCellFocus() {
		return new CellRef(_focusArea.getRow(), _focusArea.getColumn());
	}

	/**
	 * Sets the cell focus position.(this method doesn't focus the spreadsheet.)
	 * In general, if you set a cell focus, you also set the selection by
	 * {@link #setSelection(AreaRef)}; And if you want to get the focus back to
	 * spreadsheet, call {@link #focus()} to retrieve focus.
	 * 
	 * @param focus the cell focus position
	 */
	public void setCellFocus(CellRef focus) {
		if (_focusArea.getColumn() != focus.getColumn()
				|| _focusArea.getRow() != focus.getRow()) {
			if (focus.getColumn() < 0 || focus.getRow() < 0
					|| focus.getColumn() >= this.getMaxcolumns()
					|| focus.getRow() >= this.getMaxrows()) {
				throw new UiException("illegal position : " + focus.toString());
			}
			setCellFocusDirectly(focus);
		}
	}
	
	private void setCellFocusDirectly(CellRef focus) {
		_focusArea.setArea(focus.getRow(), focus.getColumn(),
				focus.getRow(), focus.getColumn());
		Map args = new HashMap();
		args.put("type", "move");
		args.put("row", focus.getRow());
		args.put("column", focus.getColumn());

		response("cellFocus" + this.getUuid(), new AuCellFocus(this, args));
	}

	/** VariableResolver to handle model's variable **/
	private class InnerVariableResolver implements VariableResolver, Serializable {
		private static final long serialVersionUID = 1L;

		public Object resolveVariable(String name) throws XelException {
			Page page = getPage();
			Object result = null;
			if (page != null) {
				result = page.getZScriptVariable(Spreadsheet.this, name);
			}
			if (result == null) {
				result = Spreadsheet.this.getAttributeOrFellow(name, true);
			}
			if (result == null && page != null) {
				result = page.getXelVariable(null, null, name, true);
			}

			return result;
		}
	}

	private class InnerFunctionMapper implements FunctionMapper, Serializable {
		private static final long serialVersionUID = 1L;

		public Collection getClassNames() {
			final Page page = getPage();
			if (page != null) {
				final FunctionMapper mapper = page.getFunctionMapper();
				if (mapper != null) {
					return new ArrayList<String>(0);
				}
			}
			return null;
		}

		public Class resolveClass(String name) throws XelException {
			final Page page = getPage();
			if (page != null) {
				final FunctionMapper mapper = page.getFunctionMapper();
				if (mapper != null) {
					return null;
				}
			}
			return null;
		}

		public Function resolveFunction(String prefix, String name)
				throws XelException {
			final Page page = getPage();
			if (page != null) {
				final FunctionMapper mapper = page.getFunctionMapper();
				if (mapper != null) {
					return mapper.resolveFunction(prefix, name);
				}
			}
			return null;
		}

	}
	
	/* DataListener to handle sheet data event */
	private class InnerModelEventDispatcher extends ModelEventDispatcher{
		private static final long serialVersionUID = 20100330164021L;

		public InnerModelEventDispatcher() {
			addEventListener(ModelEvents.ON_SHEET_ORDER_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onSheetOrderChange(event);
				}
			});
			addEventListener(ModelEvents.ON_SHEET_NAME_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onSheetNameChange(event);
				}
			});
			addEventListener(ModelEvents.ON_SHEET_CREATE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onSheetCreate(event);
				}
			});
			addEventListener(ModelEvents.ON_SHEET_DELETE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onSheetDelete(event);
				}
			});
			addEventListener(ON_MODEL_FRIEND_FOCUS_MOVE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onFriendFocusMove(event);
				}
			});
			addEventListener(ON_MODEL_FRIEND_FOCUS_DELETE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onFriendFocusDelete(event);
				}
			});
			
			addEventListener(ModelEvents.ON_CELL_CONTENT_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onCellContentChange((ModelEvent)event);
				}
			});
			addEventListener(ModelEvents.ON_CHART_CONTENT_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onChartContentChange((ModelEvent)event);
				}
			});
			addEventListener(ModelEvents.ON_DATA_VALIDATION_CONTENT_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onDataValidationContentChange((ModelEvent)event);
				}
			});
			addEventListener(ModelEvents.ON_ROW_INSERT, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onRowColumnInsertDelete(event);
				}
			});
			addEventListener(ModelEvents.ON_ROW_DELETE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onRowColumnInsertDelete(event);
				}
			});
			addEventListener(ModelEvents.ON_COLUMN_INSERT, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onRowColumnInsertDelete(event);
				}
			});
			addEventListener(ModelEvents.ON_COLUMN_DELETE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onRowColumnInsertDelete(event);
				}
			});
			addEventListener(ModelEvents.ON_ROW_COLUMN_SIZE_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onRowColumnSizeChange(event);
				}
			});
			addEventListener(ModelEvents.ON_AUTOFILTER_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onAutoFilterChange(event);
				}
			});
			addEventListener(ModelEvents.ON_MERGE_ADD, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onMergeAdd(event);
				}
			});
			
			addEventListener(ModelEvents.ON_MERGE_DELETE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onMergeDelete(event);
				}
			});
			addEventListener(ModelEvents.ON_DISPLAY_GRIDLINES_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onDisplayGridlines(event);
				}
			});
			addEventListener(ModelEvents.ON_PROTECT_SHEET_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onProtectSheet(event);
				}
			});
			addEventListener(ModelEvents.ON_CHART_ADD, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onChartAdd(event);
				}
			});
			addEventListener(ModelEvents.ON_CHART_DELETE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onChartDelete(event);
				}
			});
			addEventListener(ModelEvents.ON_CHART_UPDATE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onChartUpdate(event);
				}
			});
			
			addEventListener(ModelEvents.ON_PICTURE_ADD, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onPictureAdd(event);
				}
			});
			addEventListener(ModelEvents.ON_PICTURE_DELETE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onPictureDelete(event);
				}
			});
			addEventListener(ModelEvents.ON_PICTURE_UPDATE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event) {
					onPictureUpdate(event);
				}
			});
			addEventListener(ModelEvents.ON_FREEZE_CHANGE, new ModelEventListener() {
				@Override
				public void onEvent(ModelEvent event){
					onSheetFreeze(event);
				}
			});
			/* TODO zss 3.5
			addEventListener(SSDataEvent.ON_BOOK_EXPORT, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onBookExport((SSDataEvent)event);
				}
			});
			*/
		}
		
		private void onSheetOrderChange(ModelEvent event) { 
			Spreadsheet.this.smartUpdate("sheetLabels", getSheetLabels());
			Sheet sheet = getBook().getSheet(event.getSheet().getSheetName());
			org.zkoss.zk.ui.event.Events.postEvent(new SheetEvent(Events.ON_AFTER_SHEET_ORDER_CHANGE, Spreadsheet.this, sheet));
		}
		private void onSheetNameChange(ModelEvent event) { 
			Spreadsheet.this.smartUpdate("sheetLabels", getSheetLabels());
			Sheet sheet = getBook().getSheet(event.getSheet().getSheetName());
			org.zkoss.zk.ui.event.Events.postEvent(new SheetEvent(Events.ON_AFTER_SHEET_NAME_CHANGE, Spreadsheet.this, sheet));
		}
		
		private void onSheetCreate(ModelEvent event) {
			Spreadsheet.this.smartUpdate("sheetLabels", getSheetLabels());
			Sheet sheet = getBook().getSheet(event.getSheet().getSheetName());
			org.zkoss.zk.ui.event.Events.postEvent(new SheetEvent(Events.ON_AFTER_SHEET_CREATE, Spreadsheet.this, sheet));
		}
		
		private void onSheetDelete(ModelEvent event) {
			SBook book = getSBook();
			SSheet delSheet = event.getSheet();
			//TODO zss 3.5 clear active client cache and active range record
			
			if(delSheet == getSelectedSSheet()){
				int delIndex = (Integer)event.getData(ModelEvents.PARAM_INDEX);
				//the sheet that selected is deleted, re select another
				if(delIndex>=book.getNumOfSheet()-1){
					delIndex = book.getNumOfSheet()-1;
				}
				//if current select sheet name, euqlas the delete sheet, we should select to suggest new sheet 
				setSelectedSheet(book.getSheet(delIndex).getSheetName());//this will also update sheet label	
			}else{
				//just update sheet label
				Spreadsheet.this.smartUpdate("sheetLabels", getSheetLabels());
			}
			org.zkoss.zk.ui.event.Events.postEvent(new SheetDeleteEvent(Events.ON_AFTER_SHEET_DELETE, Spreadsheet.this, delSheet.getSheetName()));
		}
		
		private void onFriendFocusMove(ModelEvent event) {
			SSheet sheet = event.getSheet();
			if (!getSelectedSSheet().equals(sheet)){
				return;
			}
			final Focus focus = (Focus) event.getCustomData(); //other's spreadsheet's focus
			final String id = focus.getId();
			if (_selfEditorFocus!=null && !id.equals(_selfEditorFocus.getId())) {
				addOrMoveFriendFocus(id, focus.getName(), focus.getColor(), focus.getSheetId(),focus.getRow(), focus.getColumn());
				syncFriendFocus();
			}
		}
		private void onFriendFocusDelete(ModelEvent event) {
			SSheet sheet = event.getSheet();
			if (!getSelectedSSheet().equals(sheet)){
				return;
			}
			final Focus focus = (Focus) event.getCustomData(); //other's spreadsheet's focus
			final String id = focus.getId();
			if (_selfEditorFocus!=null && !id.equals(_selfEditorFocus.getId())) {
				//NOTE should remove friend color, the firend is possible back (sheet switch back)
				//TODO current we dont' have a way to remove friend color cache
				removeFriendFocus(focus.getId());
				syncFriendFocus();
			}
			
		}
		private void onChartAdd(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			final SChart chart = sheet.getChart(event.getObjectId());
			if (chart !=null){
				addChartWidget(sheet, chart);
			}
		}
		private void onChartDelete(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			deleteChartWidget(sheet, event.getObjectId());
		}
		private void onChartUpdate(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			final SChart chart = sheet.getChart(event.getObjectId());
			if (chart !=null){
				updateChartWidget(sheet, chart);
			}
		} 
		private void onPictureAdd(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			
			final String objid = event.getObjectId();
			SPicture picture = sheet.getPicture(objid);
			if(picture!=null){
				addPictureWidget(event.getSheet(), picture);
			}
		}
		private void onPictureDelete(ModelEvent event) {
			deletePictureWidget(event.getSheet(), event.getObjectId());
		}
		private void onPictureUpdate(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			
			final String objid = event.getObjectId();
			SPicture picture = sheet.getPicture(objid);
			if(picture!=null){
				updatePictureWidget(event.getSheet(), picture);
			}
		}
		
		private void onCellContentChange(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			final CellRegion region = event.getRegion();
			// ZSS-393: update cell could be not the current selected sheet.
			// e.g. co-editing, cut from other sheet... etc.
			// but has to update the client (might has cache)
			
			final int left = region.getColumn();
			final int top = region.getRow();
			final int right = region.getLastColumn();
			int bottom = region.getLastRow();
			updateCell(sheet, left, top, right, bottom);
			updateUnlockInfo();
			org.zkoss.zk.ui.event.Events.postEvent(new CellAreaEvent(
					Events.ON_AFTER_CELL_CHANGE, Spreadsheet.this, new SheetImpl(new SimpleRef<SBook>(sheet.getBook()),new SimpleRef<SSheet>(sheet))
					,top, left, bottom,right));
		}
		
		private void onChartContentChange(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			
			final String objid = event.getObjectId();
			SChart chart = sheet.getChart(objid);
			if(chart!=null){
				updateWidget(sheet, objid);
			}
		}
		
		private void onDataValidationContentChange(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			
			final String objid = event.getObjectId();
// ZSS-648: the validation could have been deleted, must notify client side			
//			SDataValidation validation = sheet.getDataValidation(objid);
			
//			if(validation!=null){
				updateDataValidation(sheet, objid);
//			}
		}

		private void onRowColumnInsertDelete(ModelEvent event) {

			// determine parameters
			boolean inserted = ModelEvents.ON_ROW_INSERT.equals(event.getName())
					|| ModelEvents.ON_COLUMN_INSERT.equals(event.getName());
			boolean isRow = ModelEvents.ON_ROW_INSERT.equals(event.getName())
					|| ModelEvents.ON_ROW_DELETE.equals(event.getName());

			// update client's row/column
			_updateCellId.next();
			SSheet sheet = event.getSheet();
			CellRegion region = event.getRegion();
			if(isRow) {
				int row = region.getRow();
				int lastRow = region.getLastRow();
				int count = region.getRowCount();
				if(inserted) {
					((ExtraCtrl)getExtraCtrl()).insertRows(sheet, row, count);
				} else {
					((ExtraCtrl)getExtraCtrl()).removeRows(sheet, row, count);
				}

				// ZSS-306 a chart doesn't shrink its size when deleting rows or columns it overlaps
				int widgetTop = row;
				int widgetBottom = inserted ? lastRow + count - 1 : lastRow;
				List<WidgetLoader> list = loadWidgetLoaders();
				for(WidgetLoader loader : list) {
					loader.onRowChange(sheet, widgetTop, widgetBottom);
				}
			} else { // column
				int col = region.getColumn();
				int lastCol = region.getLastColumn();
				int count = region.getColumnCount();
				if(inserted) {
					((ExtraCtrl)getExtraCtrl()).insertColumns(sheet, col, count);
				} else {
					((ExtraCtrl)getExtraCtrl()).removeColumns(sheet, col, count);
				}

				// ZSS-306 a chart doesn't shrink its size when deleting rows or columns it overlaps
				int widgetLeft = col;
				int widgetRight = inserted ? lastCol + count - 1 : lastCol;
				List<WidgetLoader> list = loadWidgetLoaders();
				for(WidgetLoader loader : list) {
					loader.onColumnChange(sheet, widgetLeft, widgetRight);
				}
			}
			updateUnlockInfo();
		}
		
		/*
		private void onMergeChange(SSDataEvent event) {
			final Ref rng = event.getRef();
			final Ref orng = event.getOriginalRef();
			final XSheet sheet = getSheet(orng);
			((ExtraCtrl) getExtraCtrl()).updateMergeCell(sheet, 
					rng.getLeftCol(), rng.getTopRow(), rng.getRightCol(), rng.getBottomRow(),
					orng.getLeftCol(), orng.getTopRow(), orng.getRightCol(), orng.getBottomRow());
		}
		*/
		private void onMergeAdd(ModelEvent event) {
			SSheet sheet = event.getSheet();
			//don't skip at here, let extraCtrl sync mergeMatrix helper and skip
//			if (!getSelectedXSheet().equals(sheet)){
//				releaseClientCache(sheet.getId());
//				getMergeMatrixHelper(sheet);
//				return;
//			}
			CellRegion region = event.getRegion();
			((ExtraCtrl) getExtraCtrl()).addMergeCell(sheet, 
					region.getColumn(), region.getRow(), region.getLastColumn(), region.getLastRow());

		}
		private void onMergeDelete(ModelEvent event) {
			SSheet sheet = event.getSheet();
			//don't skip at here, let extraCtrl sync mergeMatrix helper and skip
//			if (!getSelectedXSheet().equals(sheet)){
//				releaseClientCache(sheet.getId());
//				return;
//			}
			CellRegion region = event.getRegion();
			((ExtraCtrl) getExtraCtrl()).deleteMergeCell(sheet,
					region.getColumn(), region.getRow(), region.getLastColumn(), region.getLastRow());
		}
		private void onRowColumnSizeChange(ModelEvent event) {
			//TODO shall pass the range over to the client side and let client side do it; rather than iterate each column and send multiple command
			final SSheet sheet = event.getSheet();
			final CellRegion region = event.getRegion();
			if (event.isWholeColumn()) {
				final int left = region.column;
				final int right = region.lastColumn;
				for (int c = left; c <= right; ++c) {
					updateColWidth(sheet, c);
				}
				
				//ZSS-455 Chart/Image doesn't move location after change column/row width/height
				List<WidgetLoader> list = loadWidgetLoaders();
				for(WidgetLoader loader:list){
					loader.onColumnChange(sheet,left,right);
				}
				
				final AreaRef rect = ((SpreadsheetCtrl) getExtraCtrl()).getVisibleArea();
				syncFriendFocusPosition(left, rect.getRow(), rect.getLastColumn(), rect.getLastRow());
			} else if (event.isWholeRow()) {
				final int top = region.row;
				final int bottom = region.lastRow;
				for (int r = top; r <= bottom; ++r) {
					updateRowHeight(sheet, r);
				}
				
				//ZSS-455 Chart/Image doesn't move location after change column/row width/height
				List<WidgetLoader> list = loadWidgetLoaders();
				for(WidgetLoader loader:list){
					loader.onRowChange(sheet,top,bottom);
				}
				
				final AreaRef rect = ((SpreadsheetCtrl) getExtraCtrl()).getVisibleArea();
				syncFriendFocusPosition(rect.getColumn(), top, rect.getLastColumn(), rect.getLastRow());
			}
		}
		
		private void onAutoFilterChange(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			
			updateAutoFilter(sheet);
		}
		private void onDisplayGridlines(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			setDisplayGridlines((Boolean)event.getData(ModelEvents.PARAM_ENABLED));
		}
		private void onProtectSheet(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			setProtectSheet((Boolean)event.getData(ModelEvents.PARAM_ENABLED));
		}
		private void onSheetFreeze(ModelEvent event) {
			final SSheet sheet = event.getSheet();
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			//TODO
			Spreadsheet.this.invalidate();
			
			List<WidgetLoader> list = loadWidgetLoaders();
			for(WidgetLoader loader:list){
				loader.onSheetFreeze(sheet);
			}
		}
		/*
		private void onBookExport(SSDataEvent event) {
			//
			String type = (String)event.getPayload();
			if("excel".equals(type)){
				//ZSS-424 get exception when undo after save
				//POI mis handle it's model with ctmodel after export. i have to aovid undo after export a excel.
				getUndoableActionManager().clear();
			}
		}
		*/
	}
	
	//ZSS-452, release client cache when it is about to out of sync.
	private void releaseClientCache(String sheetUuid){
		if(getSelectedSheetId().equals(sheetUuid)){
			return;
		}
		response(new AuInvoke(this, "_releaseClientCache",sheetUuid));
	}
	
	private void updateColWidth(SSheet sheet, int col) {
		final int width = sheet.getColumn(col).getWidth();
		final boolean newHidden = sheet.getColumn(col).isHidden();
		HeaderPositionHelper posHelper = getColumnPositionHelper(sheet);
		HeaderPositionInfo info = posHelper.getInfo(col);
		if ((info == null && (width != posHelper.getDefaultSize() || newHidden)) || (info != null && (info.size != width || info.hidden != newHidden))) {
			int id = info == null ? _custColId.next() : info.id;
			posHelper.setInfoValues(col, width, id, newHidden, true);
			((ExtraCtrl) getExtraCtrl()).setColumnWidth(sheet, col, width, id, newHidden);
		}
	}

	private void updateRowHeight(SSheet sheet, int row) {
		final SRow rowobj = sheet.getRow(row);
		final int height = rowobj.getHeight();
		final boolean newHidden = rowobj.isHidden();
		HeaderPositionHelper posHelper = getRowPositionHelper(sheet);
		HeaderPositionInfo info = posHelper.getInfo(row);
		if ((info == null && (height != posHelper.getDefaultSize() || newHidden)) || (info != null && (info.size != height || info.hidden != newHidden))) {
			int id = info == null ? _custRowId.next() : info.id;
			posHelper.setInfoValues(row, height, id, newHidden, rowobj.isCustomHeight());
			((ExtraCtrl) getExtraCtrl()).setRowHeight(sheet, row, height, id, newHidden, rowobj.isCustomHeight());
		}
	}
	
	private ActiveRangeHelper getActiveRangeHelper() {
		ActiveRangeHelper activeRangeHelper = (ActiveRangeHelper) getAttribute(ACTIVE_RANGE_KEY);
		if (activeRangeHelper == null) {
			setAttribute(ACTIVE_RANGE_KEY, activeRangeHelper = new ActiveRangeHelper());
			return activeRangeHelper;
		}
		return activeRangeHelper;
	}
	
	private MergeMatrixHelper getMergeMatrixHelper(SSheet sheet) {
		HelperContainer<MergeMatrixHelper> helpers = (HelperContainer) getAttribute(MERGE_MATRIX_KEY);
		if (helpers == null) {
			helpers = new HelperContainer<MergeMatrixHelper>();
			setAttribute(MERGE_MATRIX_KEY, helpers);
		}
		
		final String sheetId = sheet.getId();
		MergeMatrixHelper mmhelper = helpers.getHelper(sheetId);
		int fzr = getSelectedSheetRowfreeze();
		int fzc = getSelectedSheetColumnfreeze();
		if (mmhelper == null) {
			final int sz = sheet.getNumOfMergedRegion();
			final List<int[]> mergeRanges = new ArrayList<int[]>(sz);
			for(int j = sz - 1; j >= 0; --j) {
				final CellRegion addr = sheet.getMergedRegion(j);
				mergeRanges.add(new int[] {addr.column, addr.row, addr.lastColumn, addr.lastRow});
			}
			helpers.putHelper(sheetId, mmhelper = new MergeMatrixHelper(mergeRanges, fzr, fzc));
		} else {
			mmhelper.update(fzr, fzc);
		}
		return mmhelper;
	}
	
	private HeaderPositionHelper getRowPositionHelper(SSheet sheet) {
		final HeaderPositionHelper[] helper = getPositionHelpers(sheet);
		return helper != null ? helper[0] : null;
	}
	
	private HeaderPositionHelper getColumnPositionHelper(SSheet sheet) {
		final HeaderPositionHelper[] helper = getPositionHelpers(sheet); 
		return helper != null ? helper[1] : null;
	}
	
	//[0] row position, [1] column position
	private HeaderPositionHelper[] getPositionHelpers(SSheet sheet) {
		if (sheet == null) {
			return null;
		}
		
		HelperContainer<HeaderPositionHelper> helpers = (HelperContainer) getAttribute(ROW_SIZE_HELPER_KEY);
		if (helpers == null) {
			setAttribute(ROW_SIZE_HELPER_KEY, helpers = new HelperContainer<HeaderPositionHelper>());
		}
		final String sheetId = sheet.getId();
		HeaderPositionHelper helper = helpers.getHelper(sheetId);
				
		if (helper == null) {
			int defaultSize = this.getRowheight();
			
			List<HeaderPositionInfo> infos = new ArrayList<HeaderPositionInfo>();

			Iterator<SRow> iter = sheet.getRowIterator(); 
			while(iter.hasNext()) {
				SRow row = iter.next();
				final boolean hidden = row.isHidden();
				
				final int height = row.getHeight();
				if (height != defaultSize || hidden) { //special height or hidden
					infos.add(new HeaderPositionInfo(row.getIndex(), height, _custRowId.next(), hidden, row.isCustomHeight()));
				}
			}
			
			helpers.putHelper(sheetId, helper = new HeaderPositionHelper(defaultSize, infos));
		}
		return new HeaderPositionHelper[] {helper, myGetColumnPositionHelper(sheet)};
	}

	/**
	 * Update cell data/format of selected sheet to client side, you must assign
	 * a block from left-top to right-bottom.
	 * 
	 * @param left left of block
	 * @param top top of block
	 * @param right right of block
	 * @param bottom bottom of block
	 */
	/* package */void updateCell(int left, int top, int right, int bottom) {
		updateCell(getSelectedSSheet(), left, top, right, bottom);
	}

	private void updateWidget(SSheet sheet,String objId) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		if (this.isInvalidated())
			return;// since it is invalidate, we don't need to do anymore
		
		//by our implement, object id equals to widget id
		getWidgetHandler().updateWidget(sheet, objId);
	}
	
	private void updateDataValidation(SSheet sheet,String objId) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		if (this.isInvalidated())
			return;// since it is invalidate, we don't need to do anymore
		
		
		//currently, we just update all validation (no suitable client api for now)
		
		//handle Validation, must after render("activeRange" ...)
		List<Map<String, Object>> dvs = getDataValidationHandler().loadDataValidtionJASON(getSelectedSheet());
		if (dvs != null) {
			smartUpdate("dataValidations", dvs);
		} else {
			smartUpdate("dataValidations", (String) null);
		}
	}
	
	private void updateCell(SSheet sheet, int left, int top, int right, int bottom) {
		if (this.isInvalidated())
			return;// since it is invalidate, we don't need to do anymore

		String sheetId = sheet.getId();
		if (!getActiveRangeHelper().containsSheet(sheet))
			return;
		
		left = left > 0 ? left - 1 : 0;// for border, when update a range, we
		// should also update the left - 1, top - 1 part
		top = top > 0 ? top - 1 : 0;
		
		//TODO zss 3.5, just found the logic here might be not correct, 
		//the rect and select freeze should base on sheet, not selectedSheet
		final AreaRef rect = getActiveRangeHelper().getArea(_selectedSheet);
		
		final int loadLeft = rect.getColumn();
		final int loadTop = rect.getRow();
		final int loadRight = rect.getLastColumn();
		final int loadBottom = rect.getLastRow();
		
		// ZSS-639: use corresponding sheet, not current selected sheet
		FreezeInfoLoader fil = getFreezeInfoLoader();
		final int frRow = fil.getRowFreeze(sheet);
		final int frCol = fil.getColumnFreeze(sheet);
		
		final int frTop = top <= frRow ? top : -1;
		final int frBottom = frRow;
		final int frLeft = left <= frCol ? left : -1;
		final int frRight = frCol;
		
		if (loadLeft > left) {
			left = loadLeft;
		}
		if (loadRight < right) {
			right = loadRight;
		}
		if (loadTop > top) {
			top = loadTop;
		}
		if (loadBottom < bottom) {
			bottom = loadBottom; 
		}
		
		//TODO: update freeze range
		
		// ZSS-393: update every panel separately
		SpreadsheetCtrl spreadsheetCtrl = ((SpreadsheetCtrl) this.getExtraCtrl());
		JSONObject activeRange = null;
		//	data panel 
		if (top >= 0 && top <= bottom && left >= 0 && left <= right) {
			activeRange = responseUpdateCell(sheet, sheetId, left, top, right, bottom);
		}
		// top freeze panel
		if (frTop >= 0 && frTop <= frBottom && left >= 0 && left <= right) { 
			if(activeRange == null) { // freeze panel needs payloader
				activeRange = responseUpdateCell(sheet, sheetId, loadLeft, loadTop, loadLeft, loadTop); 
			}
			activeRange.put("topFrozen", responseUpdateCell(sheet, sheetId, left, frTop, right, frBottom));
		}
		// left freeze panel
		if (frLeft >= 0 && frLeft <= frRight && top >= 0 && top <= bottom) {
			if(activeRange == null) { // freeze panel needs payloader
				activeRange = responseUpdateCell(sheet, sheetId, loadLeft, loadTop, loadLeft, loadTop); 
			}
			activeRange.put("leftFrozen", responseUpdateCell(sheet, sheetId, frLeft, top, frRight, bottom));
		}
		// corner freeze panel
		if (frTop >= 0 && frTop <= frBottom && frLeft >= 0 && frLeft <= frRight) {
			if(activeRange == null) { // freeze panel needs payloader
				activeRange = responseUpdateCell(sheet, sheetId, loadLeft, loadTop, loadLeft, loadTop);
			}
			activeRange.put("cornerFrozen", responseUpdateCell(sheet, sheetId, frLeft, frTop, frRight, frBottom));
		}
		
		if(activeRange != null) {
			response(top + "_" + left + "_" + _updateCellId.next(), new AuDataUpdate(this, "", sheetId, activeRange));
		}
	}
	
	private JSONObject responseUpdateCell(SSheet sheet, String sheetId, int left, int top, int right, int bottom) {
		SpreadsheetCtrl spreadsheetCtrl = ((SpreadsheetCtrl) this.getExtraCtrl());
		JSONObject result = spreadsheetCtrl.getRangeAttrs(sheet, SpreadsheetCtrl.Header.NONE, CellAttribute.ALL, left, top, right, bottom);
		result.put("type", "udcell");
		return result;
	}

	private HeaderPositionHelper myGetColumnPositionHelper(SSheet sheet) {
		HelperContainer<HeaderPositionHelper> helpers = (HelperContainer) getAttribute(COLUMN_SIZE_HELPER_KEY);
		if (helpers == null) {
			setAttribute(COLUMN_SIZE_HELPER_KEY, helpers = new HelperContainer<HeaderPositionHelper>());
		}
		final String sheetId = sheet.getId();
		HeaderPositionHelper helper = helpers.getHelper(sheetId);
		
		if (helper == null) {
			final int defaultColSize = sheet.getDefaultColumnWidth();
			List<HeaderPositionInfo> infos = new ArrayList<HeaderPositionInfo>();
			Iterator<SColumnArray> iter = sheet.getColumnArrayIterator(); 
			while(iter.hasNext()) {
				SColumnArray columnArray = iter.next();
				final boolean hidden = columnArray.isHidden(); //whether this column is hidden
				final int columnWidth = columnArray.getWidth();//column width
				if (columnArray.isCustomWidth() || hidden) { 
					for(int i = columnArray.getIndex(); i <= columnArray.getLastIndex();i++){
						infos.add(new HeaderPositionInfo(i, columnWidth, _custColId.next(), hidden, true));
					}
				}
			}

			helpers.putHelper(sheetId, helper = new HeaderPositionHelper(defaultColSize, infos));
		}
		return helper;
	}
	
	
	@Override
	public Object getExtraCtrl() {
		return newExtraCtrl();
	}

	/**
	 * Return a extra controller. only spreadsheet developer need to call this
	 * method.
	 */
	protected Object newExtraCtrl() {
		return new ExtraCtrl();
	}

	private class ExtraCtrl implements SpreadsheetCtrl, SpreadsheetInCtrl,
			SpreadsheetOutCtrl, DynamicMedia {
		
		public void setUserActionManagerCtrl(UserActionManagerCtrl actionHandler) {
			Spreadsheet.this.setUserActionManagerCtrl(actionHandler);
		}
		
		public UserActionManagerCtrl getUserActionManagerCtrl() {
			return Spreadsheet.this.getUserActionManagerCtrl();
		}
		
		public Media getMedia(String pathInfo) {
			return new AMedia("css", "css", "text/css;charset=UTF-8",
					getSheetDefaultRules());
		}

		public void setColumnSize(String sheetId, int column, int newsize, int id, boolean hidden) {
			SSheet xsheet;
			if (getSelectedSheetId().equals(sheetId)) {
				xsheet = getSelectedSSheet();
			} else {
				xsheet = _book.getSheetById(sheetId);
			}
			// update helper size first before sheet.setColumnWidth, or it will fire a SSDataEvent
			HeaderPositionHelper helper = Spreadsheet.this.getColumnPositionHelper(xsheet);
			helper.setInfoValues(column, newsize, id, hidden, true);

			Sheet sheet = new SheetImpl(new SimpleRef<SBook>(xsheet.getBook()),new SimpleRef<SSheet>(xsheet));
			if(sheet.isProtected()){
				return;
			}
			UndoableActionManager uam = getUndoableActionManager();
			
			if(hidden){
				uam.doAction(new HideHeaderAction(Labels.getLabel("zss.undo.hideColumn"), 
						sheet, 0, column, 0, column, HideHeaderAction.Type.COLUMN, hidden));
			}else{
				uam.doAction(
					new AggregatedAction(Labels.getLabel("zss.undo.columnSize"),
						new UndoableAction[]{
							new HideHeaderAction(null,sheet, 0, column, 0, column, HideHeaderAction.Type.COLUMN, hidden),
							new ResizeHeaderAction(null,sheet, 0, column, 0, column, ResizeHeaderAction.Type.COLUMN, newsize, true)}
					));
			}
		}

		public void setRowSize(String sheetId, int row, int newsize, int id, boolean hidden, boolean isCustom) {
			SSheet xsheet;
			if (getSelectedSheetId().equals(sheetId)) {
				xsheet = getSelectedSSheet();
			} else {
				xsheet = _book.getSheetById(sheetId);
			}

			HeaderPositionHelper helper = Spreadsheet.this.getRowPositionHelper(xsheet);
			helper.setInfoValues(row, newsize, id, hidden, isCustom);
			
			Sheet sheet = new SheetImpl(new SimpleRef<SBook>(xsheet.getBook()),new SimpleRef<SSheet>(xsheet));
			if(sheet.isProtected()){
				return;
			}
			
			UndoableActionManager uam = getUndoableActionManager();
			
			if(hidden){
				uam.doAction(new HideHeaderAction(Labels.getLabel("zss.undo.hideRow"), 
						sheet, row,0, row, 0, HideHeaderAction.Type.ROW, hidden));
			}else{
				if (isCustom){
					uam.doAction(
							new AggregatedAction(Labels.getLabel("zss.undo.rowSize"),
									new UndoableAction[]{
								new HideHeaderAction(null,sheet,  row,0, row, 0, HideHeaderAction.Type.ROW, hidden),
								new ResizeHeaderAction(null,sheet,  row,0, row, 0, ResizeHeaderAction.Type.ROW, newsize, isCustom)}
									));
				}else{ //ZSS-552 avoid auto-adjusting row height being put into undo history
					CellOperationUtil.setRowHeight(Ranges.range(sheet,row,0,row,0).toRowRange(),newsize, isCustom);
				}
			}
		}

		public HeaderPositionHelper getColumnPositionHelper(String sheetId) {
			SSheet sheet;
			if (getSelectedSheetId().equals(sheetId)) {
				sheet = getSelectedSSheet();
			} else {
				sheet = _book.getSheetById(sheetId);
			}
			HeaderPositionHelper helper = Spreadsheet.this.getColumnPositionHelper(sheet);
			return helper;
		}

		public HeaderPositionHelper getRowPositionHelper(String sheetId) {
			SSheet sheet;
			if (getSelectedSheetId().equals(sheetId)) {
				sheet = getSelectedSSheet();
			} else {
				sheet = _book.getSheetById(sheetId);
			}
			HeaderPositionHelper helper = Spreadsheet.this
					.getRowPositionHelper(sheet);
			return helper;
		}

		public MergeMatrixHelper getMergeMatrixHelper(SSheet sheet) {
			return Spreadsheet.this.getMergeMatrixHelper(sheet);
		}

		public AreaRef getSelectionArea() {
			return (AreaRef) _selectionArea.cloneSelf();
		}

		public AreaRef getFocusArea() {
			return (AreaRef) _focusArea.cloneSelf();
		}

		public void setSelectionRect(int left, int top, int right, int bottom) {
			_selectionArea.setArea(top, left, bottom, right);
		}

		public void setFocusRect(int left, int top, int right, int bottom) {
			_focusArea.setArea(top, left, bottom, right);
		}

		public AreaRef getLoadedArea() {
			AreaRef rect = getActiveRangeHelper().getArea(_selectedSheet);
			if (rect == null)
				return null;
			return (AreaRef)rect.cloneSelf();
		}

		public void setLoadedRect(int left, int top, int right, int bottom) {
			getActiveRangeHelper().setActiveRange(_selectedSheet, top, left, bottom, right);
			getWidgetHandler().onLoadOnDemand(getSelectedSSheet(), left, top, right, bottom);
		}
		
		public void setVisibleRect(int left, int top, int right, int bottom) {
			_visibleArea.setArea(top, left, bottom, right);
			getWidgetHandler().onLoadOnDemand(getSelectedSSheet(), left, top, right, bottom);
		}

		public AreaRef getVisibleArea() {
			return (AreaRef) _visibleArea.cloneSelf();
		}

		public boolean addWidget(Widget widget) {
			return Spreadsheet.this.addWidget(widget);
		}

		public boolean removeWidget(Widget widget) {
			return Spreadsheet.this.removeWidget(widget);
		}
		
		public WidgetHandler getWidgetHandler() {
			return Spreadsheet.this.getWidgetHandler();
		}
		
		public JSONObject getRowHeaderAttrs(SSheet sheet, int rowStart, int rowEnd) {
			return getHeaderAttrs(sheet, true, rowStart, rowEnd);
		}
		
		public JSONObject getColumnHeaderAttrs(SSheet sheet, int colStart, int colEnd) {
			return getHeaderAttrs(sheet, false, colStart, colEnd);
		}
		
		/**
		 * Header attributes
		 * 
		 * <ul>
		 * 	<li>t: header type</li>
		 *  <li>s: index start</li>
		 *  <li>e: index end</li>
		 *  <li>hs: headers, a JSONArray object</li>
		 * </ul>
		 * 
		 * @param isRow
		 * @param start
		 * @param end
		 * @return
		 */
		private JSONObject getHeaderAttrs(SSheet sheet, boolean isRow, int start, int end) {
			JSONObject attrs = new JSONObject();
			attrs.put("s", start);
			attrs.put("e", end);
			
			JSONArray headers = new JSONArray();
			attrs.put("hs", headers);
			if (isRow) {
				attrs.put("t", "r"); //type: row
				for (int row = start; row <= end; row++) {
					headers.add(getRowHeaderAttrs(sheet, row));
				}
			} else { //column header
				attrs.put("t", "c"); //type: column
				for (int col = start; col <= end; col++) {
					headers.add(getColumnHeaderAttrs(sheet, col));
				}
			}
			return attrs;
		}
		
		/**
		 * Column header attributes
		 * 
		 * <ul>
		 * 	<li>i: column index</li>
		 *  <li>t: title</li>
		 *  <li>p: position info id</li>
		 * </ul>
		 * 
		 * Ignore attribute if it's default
		 * Default attributes
		 * <ul>
		 * 	<li>hidden: false</li>
		 * </ul>
		 * 
		 * @return
		 */
		private JSONObject getColumnHeaderAttrs(SSheet sheet, int col) {
			JSONObject attrs = new JSONObject();
//			attrs.put("i", col);//getHeaderAttrs method has provide index info
			attrs.put("t", Spreadsheet.this.getColumntitle(col));

			HeaderPositionHelper colHelper = Spreadsheet.this.getColumnPositionHelper(sheet);
			HeaderPositionInfo info = colHelper.getInfo(col);
			if (info != null) {
				attrs.put("p", info.id);
//				if (info.size != defaultSize) {
//					attrs.put("s", info.size);
//				}
//				if (info.hidden) {
//					attrs.put("h", 1); //1 stand for true;
//				}
			}
			return attrs;
		}
		
		/**
		 * Row header attributes
		 * 
		 * <ul>
		 * 	<li>i: row index</li>
		 *  <li>t: title</li>
		 *  <li>p: position info id</li>
		 * </ul>
		 * 
		 * Ignore attribute if it's default
		 * Default attributes
		 * <ul>
		 * 	<li>hidden: false</li>
		 * </ul>
		 * 
		 * @return
		 */
		private JSONObject getRowHeaderAttrs(SSheet sheet, int row) {
			JSONObject attrs = new JSONObject();
//			attrs.put("i", row);//getHeaderAttrs method has provide index info
			attrs.put("t", Spreadsheet.this.getRowtitle(row));

			HeaderPositionHelper rowHelper = Spreadsheet.this.getRowPositionHelper(sheet);
			HeaderPositionInfo info = rowHelper.getInfo(row);
			if (info != null) {
				attrs.put("p", info.id);
//				if (info.hidden)
//					attrs.put("h", 1);
			}
			return attrs;
		}

		/**
		 * Range attributes
		 * 
		 * <ul>
		 * 	<li>id: sheet uuid</li>
		 * 	<li>l: range top</li>
		 *  <li>t: range top</li>
		 *  <li>r: range right</li>
		 *  <li>b: range bottom</li>
		 *  <li>at: range update Attribute Type</li>
		 *  <li>rs: rows, a JSONArray object</li>
		 *  <li>cs: cells, a JSONArray object</li>
		 * 	<li>s: strings, a JSONArray object</li>
		 *  <li>st: styles, a JSONArray object</li>
		 *  <li>m: merge attributes</li>
		 *  <li>rhs: row headers, a JSONArray object</li>
		 *  <li>chs: column headers, a JSONArray object</li>
		 * </ul>
		 * 
		 * @param left
		 * @param top
		 * @param right
		 * @param bottom
		 * @param containsHeader
		 * @return
		 */
		public JSONObject getRangeAttrs(SSheet sheet, Header containsHeader, CellAttribute type, int left, int top, int right, int bottom) {
			JSONObject attrs = new JSONObject();
			
			attrs.put("id", sheet.getId());
			
			attrs.put("l", left);
			attrs.put("t", top);
			attrs.put("r", right);
			attrs.put("b", bottom);
			attrs.put("at", type);
			
			JSONArray rows = new JSONArray();
			attrs.put("rs", rows);
			
			StringAggregation styleAggregation = new StringAggregation();
			StringAggregation textAggregation = new StringAggregation();
			MergeAggregation mergeAggregation = new MergeAggregation(getMergeMatrixHelper(sheet));
			for (int row = top; row <= bottom; row++) {
				JSONObject r = getRowAttrs(row);
				rows.add(r);
				
				JSONArray cells = new JSONArray();
				r.put("cs", cells);
				for (int col = left; col <= right; col++) {
					cells.add(getCellAttr(sheet, type, row, col, styleAggregation, textAggregation, mergeAggregation));
				}
			}
			
			attrs.put("s", textAggregation.getJSONArray());
			attrs.put("st", styleAggregation.getJSONArray());
			attrs.put("m", mergeAggregation.getJSONObject());
			
			boolean addRowColumnHeader = containsHeader == Header.BOTH;
			boolean addRowHeader = addRowColumnHeader || containsHeader == Header.ROW;
			boolean addColumnHeader = addRowColumnHeader || containsHeader == Header.COLUMN;
			
			if (addRowHeader)
				attrs.put("rhs", getRowHeaderAttrs(sheet, top, bottom));
			if (addColumnHeader)
				attrs.put("chs", getColumnHeaderAttrs(sheet, left, right));
			
			return attrs;
		}
		
		/**
		 * Row attributes
		 * <ul>
		 * 	<li>r: row number</li>
		 *  <li>h: height index</li>
		 *  <li>hd: hidden</li>
		 * </ul>
		 * 
		 * Ignore if attribute is default
		 * <ul>
		 * 	<li>hidden: default is false</li>
		 * </ul>
		 */
		public JSONObject getRowAttrs(int row) {
			SSheet sheet = getSelectedSSheet();
			HeaderPositionHelper helper = Spreadsheet.this.getRowPositionHelper(sheet);
			JSONObject attrs = new JSONObject();
			//row num
			attrs.put("r", row);
			
			HeaderPositionInfo info = helper.getInfo(row);
			if (info != null) {
				attrs.put("h", info.id);
				if (info.hidden) {
					attrs.put("hd", "t"); //t stand for true
				}
			}
			return attrs;
		}

		/**
		 * Cell attributes
		 * 
		 * <ul>
		 * 	<li>r: row number</li>
		 *  <li>c: column number</li>
		 *  <li>t: cell html text</li>
		 *  <li>et: cell edit text</li>
		 *  <li>ft: format text</li>
		 *  <li>meft: merge cell html text, edit text and format text</li>
		 *  <li>ct: cell type</li>
		 *  <li>s: cell style</li>
		 *  <li>is: cell inner style</li>
		 *  <li>rb: cell right border</li>
		 *  <li>l: locked</>
		 *  <li>wp: wrap</li>
		 *  <li>ha: horizontal alignment</>
		 *  <li>va: vertical alignment</>
		 *  <li>mi: merge id index</li>
		 *  <li>mc: merge CSS index</li>
		 *  <li>fs: font size</li>
		 *  <li>ovf: overflow</li>
		 * </ul>
		 * 
		 * Ignore put attribute if it's default
		 * Default attributes
		 * <ul>
		 * 	<li>Cell type: blank</>
		 *  <li>Locked: true</>
		 *  <li>Wrap: false</li>
		 *  <li>Horizontal alignment: left</>
		 *  <li>Vertical alignment: top</>
		 *  <li>Overflow: false</li>
		 *  <li>Font size: 11pt</>
		 * </ul>
		 */
		public JSONObject getCellAttr(SSheet sheet, CellAttribute type, int row, int col, StringAggregation styleAggregation, StringAggregation textAggregation, MergeAggregation mergeAggregation) {
			boolean updateAll = type == CellAttribute.ALL,
				updateText = (updateAll || type == CellAttribute.TEXT),
				updateStyle = (updateAll || type == CellAttribute.STYLE),
				updateSize = (updateAll || type == CellAttribute.SIZE),
				updateMerge = (updateAll || type == CellAttribute.MERGE);
			
			SCell cell = sheet.getCell(row, col);
			JSONObject attrs = new JSONObject();
			
			//row num, cell num attr
//			if (cell != null) {
//				attrs.put("r", row);
//				attrs.put("c", col);
//			}
			
			//merge
			MergeIndex mergeIndex = mergeAggregation.add(row, col);
			if (updateMerge && mergeIndex != null) {
				attrs.put("mi", mergeIndex.getMergeId());
				attrs.put("mc", mergeIndex.getMergeCSSId());
			}
			
			//width, height id
			if (updateSize) {
				if (cell != null) {
					//process overflow when cell type is string, halign is left, no wrap, no merge
					SCellStyle cellStyle = cell.getCellStyle();
					if (cell.getType() == CellType.STRING && 
						mergeIndex == null && !cellStyle.isWrapText() &&
								CellFormatHelper.getRealAlignment(cell) == SCellStyle.Alignment.LEFT) {

						// ZSS-224: modify overflow flag spec. to carry more status in bitswise format
						// 1: needs overflow
						// 2: skip overflow when initializing
						// 4: [undefined now]
						// 8: ...
						int overflowOptions = 1; // needs overflow
						
						// ZSS-224: pre-check sibling cell's status and give current cell a hint 
						// to process overflow or not when initializing. 
						SCell sibling = sheet.getCell(row, col + 1);
						if(sibling.getType() != CellType.BLANK) {
							overflowOptions |= 2; // skip overflow when initializing
						}
						
						// appy to response
						attrs.put("ovf", overflowOptions); 
					}
				}
			}
			SCellStyle cellStyle = sheet.getCell(row, col).getCellStyle();
			CellFormatHelper cfh = new CellFormatHelper(sheet, row, col, getMergeMatrixHelper(sheet));
			//style attr
			if (updateStyle) {
				String style = cfh.getHtmlStyle();
				if (!Strings.isEmpty(style)) {
					int idx = styleAggregation.add(style);
					attrs.put("s", idx);
				}
				String innerStyle = cfh.getInnerHtmlStyle();
				if (!Strings.isEmpty(innerStyle)) {
					int idx = styleAggregation.add(innerStyle);
					attrs.put("is", idx);
				}
				if (cfh.hasRightBorder()) {
					attrs.put("rb", 1); 
				}
				
				//ZSS509, handling lock info even cell is null ( lock in row,column style)
				boolean locked = cellStyle.isLocked();
				if (!locked)
					attrs.put("l", "f"); //f stand for "false"
			}
			
			if (!cell.isNull()) {
				CellType cellType = cell.getType();
				if (cellType != CellType.BLANK)
					attrs.put("ct", cellType.value());
				
				if (updateText) {
					if (cellType != CellType.BLANK || cell.getHyperlink() != null) {
						String cellText = getCellDisplayLoader().getCellHtmlText(sheet, row, col);
						final String editText = cfh.getCellEditText();
						final String formatText = cfh.getCellFormattedText();
						
						if (Objects.equals(cellText, editText) && Objects.equals(editText, formatText)) {
							attrs.put("meft", textAggregation.add(cellText));
						} else {
							attrs.put("t", textAggregation.add(cellText));
							attrs.put("et", textAggregation.add(editText));
							attrs.put("ft", textAggregation.add(formatText));
						}
					}
				}
				
				if (updateStyle) {
					boolean wrap = cellStyle.isWrapText();
					if (wrap)
						attrs.put("wp", 1);
					
					Alignment horizontalAlignment = CellFormatHelper.getRealAlignment(cell);
					switch(horizontalAlignment) {
					case CENTER:
					case CENTER_SELECTION:
						attrs.put("ha", "c");
						break;
					case RIGHT:
						attrs.put("ha", "r");
						break;
					}
					
					VerticalAlignment verticalAlignment = cellStyle.getVerticalAlignment();
					switch(verticalAlignment) {
					case TOP:
						attrs.put("va", "t");
						break;
					case CENTER:
						attrs.put("va", "c");
						break;
					//case CellStyle.VERTICAL_BOTTOM: //default
					//	break;
					}
					
					SFont font = cellStyle.getFont();
					int fontSize = font.getHeightPoints();
					attrs.put("fs", fontSize);
				}
			}
			return attrs;
		}

		public void insertColumns(SSheet sheet, int col, int size) {
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			if (size <= 0) {
				throw new UiException("size must > 0 : " + size);
			}
			if (col > _maxColumns) {// not in scope, do nothing,
				return;
			}
			// because of rowfreeze and colfreeze,
			// don't avoid insert behavior here, always send required data to
			// client,
			// let client handle it

			HashMap result = new HashMap();
			result.put("type", "column");
			result.put("col", col);
			result.put("size", size);

			final AreaRef rect = getActiveRangeHelper().getArea(_selectedSheet);
			int right = size + rect.getLastColumn();
			
			HeaderPositionHelper colHelper = Spreadsheet.this.getColumnPositionHelper(sheet);
			colHelper.shiftMeta(col, size);
			result.put("hs", getColumnHeaderAttrs(_selectedSheet, col, right));

			//_maxColumns += size;
//			int cf = getColumnfreeze();
//			if (cf >= col) {
//				_colFreeze += size;
//			}

			result.put("maxcol", _maxColumns);
			result.put("colfreeze", getSelectedSheetColumnfreeze());

			response("insertRowColumn" + XUtils.nextUpdateId(), new AuInsertRowColumn(Spreadsheet.this, "", sheet.getId(), result));

			rect.setLastColumn(right);

			// update surround cell
			int left = col;
			right = left + size - 1;
			right = right >= _maxColumns - 1 ? _maxColumns - 1 : right;
			int top = rect.getRow();
			int bottom = rect.getLastRow();
			if(log.debugable()){
				log.debug("update cells when insert column " + col + ",size:" + size + ":" + left + "," + top + "," + right + "," + bottom);
			}
			updateCell(sheet, left, top, right, bottom);
			
			// ZSS-404: must update cell in freeze panels (previous range is only for data block)
			int rowFreeze = getSelectedSheetRowfreeze();
			if(rowFreeze >= 0) {
				updateCell(sheet, left, 0, right, rowFreeze); // top freeze panel
			}

			//update inserted column widths
			updateColWidths(sheet, col, size); 
			
		}

		private void updateRowHeights(SSheet sheet, int row, int n) {
			for(int r = 0; r < n; ++r) {
				updateRowHeight(sheet, r+row);
			}
		}
		
		private void updateColWidths(SSheet sheet, int col, int n) {
			for(int r = 0; r < n; ++r) {
				updateColWidth(sheet, r+col);
			}
		}
		public void insertRows(SSheet sheet, int row, int size) {
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			if (size <= 0) {
				throw new UiException("size must > 0 : " + size);
			}
			if (row > _maxRows) {// not in scrope, do nothing,
				return;
			}
			// because of rowfreeze and colfreeze,
			// don't avoid insert behavior here, always send required data to
			// client,
			// let client handle it

			HashMap result = new HashMap();
			result.put("type", "row");
			result.put("row", row);
			result.put("size", size);

			final AreaRef rect = getActiveRangeHelper().getArea(_selectedSheet);
			int bottom = size + rect.getLastRow();

			HeaderPositionHelper rowHelper = Spreadsheet.this.getRowPositionHelper(sheet);
			rowHelper.shiftMeta(row, size);
			
			result.put("hs", getRowHeaderAttrs(_selectedSheet, row, bottom));
			
			//_maxRows += size;

			result.put("maxrow", _maxRows);
			result.put("rowfreeze", getSelectedSheetRowfreeze());

			response("insertRowColumn" + XUtils.nextUpdateId(), new AuInsertRowColumn(Spreadsheet.this, "", sheet.getId(), result));

			rect.setLastRow(bottom);

			// update surround cell
			int top = row;
			bottom = top + size - 1;
			bottom = bottom >= _maxRows - 1 ? _maxRows - 1 : bottom;
			int left = rect.getColumn();
			int right = rect.getLastColumn();
			
			log.debug("update cells when insert row " + row + ",size:" + size + ":" + left + "," + top + "," + right + "," + bottom);
			updateCell(sheet, left, top, right, bottom);
			
			// ZSS-404: must update cell in freeze panels (previous range is only for data block)
			int colFreeze = getSelectedSheetColumnfreeze();
			if(colFreeze >= 0) {
				updateCell(sheet, 0, top, colFreeze, bottom); // left freeze panel
			}
			
			// update the inserted row height
			updateRowHeights(sheet, row, size); //update row height
		}

		public void removeColumns(SSheet sheet, int col, int size) {
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			if (size <= 0) {
				throw new UiException("size must > 0 : " + size);
			} else if (col < 0) {
				throw new UiException("column must >= 0 : " + col);
			}
			if (col >= _maxColumns) {
				return;
			}

			if (col + size > _maxColumns) {
				size = _maxColumns - col;
			}

			// because of rowfreeze and colfreeze,
			// don't avoid insert behavior here, always send required data to
			// client,
			// let client handle it


			HashMap result = new HashMap();
			result.put("type", "column");
			result.put("col", col);
			result.put("size", size);

			final AreaRef rect = getActiveRangeHelper().getArea(_selectedSheet);
			int right = rect.getLastColumn() - size;
			if (right < col) {
				right = col - 1;
			}
		
			HeaderPositionHelper colHelper = Spreadsheet.this.getColumnPositionHelper(sheet);
			colHelper.unshiftMeta(col, size);
			
			result.put("hs", getColumnHeaderAttrs(_selectedSheet, col, right));
		

			//_maxColumns -= size;

			result.put("maxcol", _maxColumns);
			result.put("colfreeze", getSelectedSheetColumnfreeze());

			response("removeRowColumn" + XUtils.nextUpdateId(), new AuRemoveRowColumn(Spreadsheet.this, "", sheet.getId(), result));
			rect.setLastColumn(right);

			// update surround cell
			int left = col;
			right = left;
			
			updateCell(sheet, left, rect.getRow(), right, rect.getLastRow());
		}

		public void removeRows(SSheet sheet, int row, int size) {
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			if (size <= 0) {
				throw new UiException("size must > 0 : " + size);
			} else if (row < 0) {
				throw new UiException("row must >= 0 : " + row);
			}
			if (row >= _maxRows) {
				return;
			}
			if (row + size > _maxRows) {
				size = _maxRows - row;
			}

			// because of rowfreeze and colfreeze,
			// don't avoid insert behavior here, always send required data to
			// client,
			// let client handle it

			HashMap result = new HashMap();
			result.put("type", "row");
			result.put("row", row);
			result.put("size", size);
			
			final AreaRef rect = getActiveRangeHelper().getArea(_selectedSheet);
			int bottom = rect.getLastRow() - size;
			if (bottom < row) {
				bottom = row - 1;
			}
			
			HeaderPositionHelper rowHelper = Spreadsheet.this.getRowPositionHelper(sheet);
			rowHelper.unshiftMeta(row, size);
			
			result.put("hs", getRowHeaderAttrs(_selectedSheet, row, bottom));

//			_maxRows -= size;

			result.put("maxrow", _maxRows);
			result.put("rowfreeze", getSelectedSheetRowfreeze());

			response("removeRowColumn" + XUtils.nextUpdateId(), new AuRemoveRowColumn(Spreadsheet.this, "", sheet.getId(), result));
			rect.setLastRow(bottom);

			// update surround cell
			int top = row;
			bottom = top;
			
			updateCell(sheet, rect.getColumn(), top, rect.getLastColumn(), bottom);
		}

		public void updateMergeCell(SSheet sheet, int left, int top, int right,
				int bottom, int oleft, int otop, int oright, int obottom) {
			deleteMergeCell(sheet, oleft, otop, oright, obottom);
			addMergeCell(sheet, left, top, right, bottom);
		}

		public void deleteMergeCell(SSheet sheet, int left, int top, int right, int bottom) {
			MergeMatrixHelper mmhelper = this.getMergeMatrixHelper(sheet);
			Set torem = new HashSet();
			mmhelper.deleteMergeRange(left, top, right, bottom, torem);
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			for (Iterator iter = torem.iterator(); iter.hasNext();) {
				MergedRect rect = (MergedRect) iter.next();

				updateMergeCell0(sheet, rect, "remove");
			}
			//updateCell(sheet, left > 0 ? left - 1 : 0, top > 1 ? top - 1 : 0, right + 1, bottom + 1);
			updateCell(sheet, left, top, right, bottom);
		}

		private void updateMergeCell0(SSheet sheet, MergedRect block, String type) {
			JSONObj result = new JSONObj();
			result.setData("type", type);
			result.setData("id", block.getId());
			int left = block.getColumn();
			int top = block.getRow();
			int right = block.getLastColumn();
			int bottom = block.getLastRow();

			// don't check range to ignore update case,
			// because I still need to sync merge cell data to client side

			result.setData("left", left);
			result.setData("top", top);
			result.setData("right", right);
			result.setData("bottom", bottom);

			HeaderPositionHelper helper = Spreadsheet.this
					.getColumnPositionHelper(sheet);
			final int w = helper.getStartPixel(block.getLastColumn() + 1) - helper.getStartPixel(block.getColumn());
			result.setData("width", w);

			HeaderPositionHelper rhelper = Spreadsheet.this
					.getRowPositionHelper(sheet);
			final int h = rhelper.getStartPixel(block.getLastRow() + 1) - rhelper.getStartPixel(block.getRow());
			result.setData("height", h);

			/**
			 * merge_ -> mergeCell
			 */
			response("mergeCell" + XUtils.nextUpdateId(), new AuMergeCell(Spreadsheet.this, "", sheet.getId(), result.toString()));
		}

		public void addMergeCell(SSheet sheet, int left, int top, int right, int bottom) {
			MergeMatrixHelper mmhelper = this.getMergeMatrixHelper(sheet);

			Set toadd = new HashSet();
			Set torem = new HashSet();
			mmhelper.addMergeRange(left, top, right, bottom, toadd, torem);
			
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			
			for (Iterator iter = torem.iterator(); iter.hasNext();) {
				MergedRect rect = (MergedRect) iter.next();
				log.debug("(A)remove merge:" + rect);
				updateMergeCell0(sheet, rect, "remove");
			}
			for (Iterator iter = toadd.iterator(); iter.hasNext();) {
				MergedRect rect = (MergedRect) iter.next();
				log.debug("add merge:" + rect);
				updateMergeCell0(sheet, rect, "add");
			}
//			updateCell(sheet, left > 0 ? left - 1 : 0, top > 1 ? top - 1 : 0, right + 1, bottom + 1);
			updateCell(sheet, left, top, right, bottom);
		}

		//in pixel
		public void setColumnWidth(SSheet sheet, int col, int width, int id, boolean hidden) {
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			JSONObject result = new JSONObject();
			result.put("type", "column");
			result.put("column", col);
			result.put("width", width);
			result.put("id", id);
			result.put("hidden", hidden);
			smartUpdate("columnSize", (Object) new Object[] { "", sheet.getId(), result}, true);
		}

		//in pixels
		public void setRowHeight(SSheet sheet, int row, int height, int id, boolean hidden, boolean isCustom) {
			if (!getSelectedSSheet().equals(sheet)){
				releaseClientCache(sheet.getId());
				return;
			}
			JSONObject result = new JSONObject();
			result.put("type", "row");
			result.put("row", row);
			result.put("height", height);
			result.put("id", id);
			result.put("hidden", hidden);
			result.put("custom", isCustom);
			smartUpdate("rowSize", (Object) new Object[] { "", sheet.getId(), result}, true);
		}

		@Override
		public Boolean getLeftHeaderHiddens(int row) {
			SSheet sheet = getSelectedSSheet();
			HeaderPositionHelper rowHelper = Spreadsheet.this
					.getRowPositionHelper(sheet);
			HeaderPositionInfo info = rowHelper.getInfo(row);
			return info == null ? Boolean.FALSE : Boolean.valueOf(info.hidden);
		}

		@Override
		public Boolean getTopHeaderHiddens(int col) {
			SSheet sheet = getSelectedSSheet();
			HeaderPositionHelper colHelper = Spreadsheet.this
					.getColumnPositionHelper(sheet);
			HeaderPositionInfo info = colHelper.getInfo(col);
			return info == null ? Boolean.FALSE : Boolean.valueOf(info.hidden);
		}

		@Override
		public void setSelectedSheetDirectly(String name,
				boolean cacheInClient, int row, int col, int left, int top,
				int right, int bottom, int highlightLeft, int highlightTop,
				int highlightRight, int highlightBottom, int rowfreeze,
				int colfreeze) {
			Spreadsheet.this.setSelectedSheetDirectly(name, cacheInClient, row, col, left,
					top, right, bottom/*, highlightLeft, highlightTop,
					highlightRight, highlightBottom, rowfreeze, colfreeze*/);
		}

		@Override
		public FreezeInfoLoader getFreezeInfoLoader() {
			return Spreadsheet.this.getFreezeInfoLoader();
		}
	}

	public void invalidate() {
		super.invalidate();
		doInvalidate();
	}

	/**
	 * Retrieve client side spreadsheet focus.The cell focus and selection will
	 * keep at last status. It is useful if you want get focus back to
	 * spreadsheet after do some outside processing, for example after user
	 * click a outside button or menu item.
	 */
	public void focus() {
		// retrieve focus should work when spreadsheet init or after invalidate.
		// so I use response to implement it.
		JSONObj result = new JSONObj();
		result.setData("type", "retrive");
		
		/**
		 * rename zssfocus -> doRetrieveFocusCmd
		 */
		response("retrieveFocus" + this.getUuid(), new AuRetrieveFocus(this, result.toString()));
	}

	/**
	 * Retrieve client side spreadhsheet focus, move cell focus to position
	 * (row,column) and also scroll the cell to into visible view.
	 * 
	 * @param row row of cell to move
	 * @param column column of cell to move
	 */
	public void focusTo(int row, int column) {
		Map args = new HashMap();
		args.put("row", row);
		args.put("column", column);
		args.put("type", "moveto");
		
		response("cellFocusTo" + this.getUuid(), new AuCellFocusTo(this, args));

		_focusArea.setColumn(column);
		_focusArea.setLastColumn(column);
		_focusArea.setRow(row);
		_focusArea.setLastRow(row);
		_selectionArea.setColumn(column);
		_selectionArea.setLastColumn(column);
		_selectionArea.setRow(row);
		_selectionArea.setLastRow(row);
	}

	private String getSheetDefaultRules() {

		SSheet sheet = getSelectedSSheet();

		HeaderPositionHelper colHelper = this.getColumnPositionHelper(sheet);
		HeaderPositionHelper rowHelper = this.getRowPositionHelper(sheet);
		MergeMatrixHelper mmhelper = this.getMergeMatrixHelper(sheet);

		boolean hiderow = isHiderowhead();
		boolean hidecol = isHidecolumnhead();
		boolean showgrid = sheet.getViewInfo().isDisplayGridlines();

		int th = hidecol ? 1 : this.getTopheadheight();
		int lw = hiderow ? 1 : this.getLeftheadwidth();
		int cp = this._cellpadding;//
		int rh = this.getRowheight();
		int cw = this.getColumnwidth();
		int lh = 20;// default line height;

		if (lh > rh) {
			lh = rh;
		}

		String sheetPrefix = " .s" + getSelectedSheetId();
		String name = "#" + getUuid();

		int cellwidth;// default
		int cellheight;// default
		Execution exe = Executions.getCurrent();

//		boolean isGecko = exe.isGecko();
		//boolean isIE = exe.isExplorer();
		//boolean isIE7 = exe.isExplorer7();

		cellwidth = cw;
		cellheight = rh;

		int celltextwidth = cw - 2 * cp;

		StringBuffer sb = new StringBuffer();

		// zcss.setRule(name+" .zsdata",["padding-top","padding-left"],[th+"px",lw+"px"],true,sid);
		sb.append(name).append(" .zsdata{");
		sb.append("padding-top:").append(th).append("px;");
		sb.append("padding-left:").append(lw).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zsrow","height",rh+"px",true,sid);
		sb.append(name).append(" .zsrow{");
		sb.append("height:").append(rh).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zscell",["padding","height","width","line-height"],["0px "+cp+"px 0px "+cp+"px",cellheight+"px",cellwidth+"px",lh+"px"],true,sid);
		sb.append(name).append(" .zscell{");
		sb.append("padding:").append("0px " + cp + "px 0px " + cp + "px;");
		sb.append("height:").append(cellheight).append("px;");
		sb.append("width:").append(cellwidth).append("px;");
		if (!showgrid) {
			sb.append("border-bottom:1px solid #FFFFFF;")
			  .append("border-right:1px solid #FFFFFF;");
		}
		// sb.append("line-height:").append(lh).append("px;\n");
		sb.append("}");

		// zcss.setRule(name+" .zscelltxt",["width","height"],[celltextwidth+"px",cellheight+"px"],true,sid);
		sb.append(name).append(" .zscelltxt{");
		sb.append("width:").append(celltextwidth).append("px;");
		sb.append("height:").append(cellheight).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zstop",["left","height","line-height"],[lw+"px",(th-2)+"px",lh+"px"],true,sid);

		int toph = th;
		int topheadh = toph;
		int cornertoph = th;
		// int topblocktop = toph;
		int fzr = getSelectedSheetRowfreeze();
		int fzc = getSelectedSheetColumnfreeze();

		if (fzr > -1) {
			toph = toph + rowHelper.getStartPixel(fzr + 1);
		}

		sb.append(name).append(" .zstop{");
		sb.append("left:").append(lw).append("px;");
		sb.append("height:").append(fzr > -1 ? toph - 1 : toph).append("px;");
		// sb.append("line-height:").append(toph).append("px;\n");
		sb.append("}");

		sb.append(name).append(" .zstopi{");
		sb.append("height:").append(toph).append("px;");
		sb.append("}");

		sb.append(name).append(" .zstophead{");
		sb.append("height:").append(topheadh).append("px;");
		sb.append("}");

		sb.append(name).append(" .zscornertop{");
		sb.append("left:").append(lw).append("px;");
		sb.append("height:").append(cornertoph).append("px;");
		sb.append("}");

		// relative, so needn't set top position.
		/*
		 * sb.append(name).append(" .zstopblock{\n");
		 * sb.append("top:").append(topblocktop).append("px;\n");
		 * sb.append("}\n");
		 */

		// zcss.setRule(name+" .zstopcell",["padding","height","width","line-height"],["0px "+cp+"px 0px "+cp+"px",th+"px",cellwidth+"px",lh+"px"],true,sid);
		sb.append(name).append(" .zstopcell{");
		sb.append("padding:").append("0px " + cp + "px 0px " + cp + "px;");
		sb.append("height:").append(topheadh).append("px;");
		sb.append("width:").append(cellwidth).append("px;");
		sb.append("line-height:").append(topheadh).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zstopcelltxt","width", celltextwidth
		// +"px",true,sid);
		sb.append(name).append(" .zstopcelltxt{");
		sb.append("width:").append(celltextwidth).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zsleft",["top","width"],[th+"px",(lw-2)+"px"],true,sid);

		int leftw = lw-1;//isGecko ? lw : lw - 1;
		int leftheadw = leftw;
		int leftblockleft = leftw;

		if (fzc > -1) {
			leftw = leftw + colHelper.getStartPixel(fzc + 1);
		}

		sb.append(name).append(" .zsleft{");
		sb.append("top:").append(th).append("px;");
		sb.append("width:").append(fzc > -1 ? leftw - 1 : leftw)
				.append("px;");
		sb.append("}");

		sb.append(name).append(" .zslefti{");
		sb.append("width:").append(leftw).append("px;");
		sb.append("}");

		sb.append(name).append(" .zslefthead{");
		sb.append("width:").append(leftheadw).append("px;");
		sb.append("}");

		sb.append(name).append(" .zsleftblock{");
		sb.append("left:").append(leftblockleft).append("px;");
		sb.append("}");

		sb.append(name).append(" .zscornerleft{");
		sb.append("top:").append(th+1).append("px;");
		sb.append("width:").append(leftheadw).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zsleftcell",["height","line-height"],[(rh-1)+"px",(rh)+"px"],true,sid);//for
		// middle the text, i use row leight instead of lh
		sb.append(name).append(" .zsleftcell{");
		sb.append("height:").append(rh).append("px;");
		sb.append("line-height:").append(rh).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zscorner",["width","height"],[(lw-2)+"px",(th-2)+"px"],true,sid);

		sb.append(name).append(" .zscorner{");
		sb.append("width:").append(fzc > -1 ? leftw : leftw + 1)
				.append("px;");
		sb.append("height:").append(fzr > -1 ? toph : toph + 1).append("px;");
		sb.append("}");

		sb.append(name).append(" .zscorneri{");
		sb.append("width:").append(lw - 2).append("px;");
		sb.append("height:").append(th - 1).append("px;");
		sb.append("}");

		sb.append(name).append(" .zscornerblock{");
		sb.append("left:").append(lw).append("px;");
		sb.append("top:").append(th+1).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zshboun","height",th+"px",true,sid);
		sb.append(name).append(" .zshboun{");
		sb.append("height:").append(th).append("px;");
		sb.append("}");

		// zcss.setRule(name+" .zshboun","height",th+"px",true,sid);
/*		sb.append(name).append(" .zshboun{\n");
		sb.append("height:").append(th).append("px;\n");
		sb.append("}\n");
*/
		// zcss.setRule(name+" .zshbouni","height",th+"px",true,sid);
		sb.append(name).append(" .zshbouni{");
		sb.append("height:").append(th).append("px;");
		sb.append("}");

		sb.append(name).append(" .zsfztop{");
		sb.append("border-bottom-style:").append(fzr > -1 ? "solid" : "none").append(";");
		sb.append("}");
		sb.append(name).append(" .zsfztop .zswidgetpanel{");
		sb.append("left:").append(-lw).append("px;");
		sb.append("}");
		
		sb.append(name).append(" .zsfzcorner{");
		sb.append("border-bottom-style:").append(fzr > -1 ? "solid" : "none").append(";");
		sb.append("}");
		sb.append(name).append(" .zsfzcorner .zswidgetpanel{");
		sb.append("left:1px;");
		sb.append("top:1px;");
		sb.append("}");

		sb.append(name).append(" .zsfzleft{");
		sb.append("border-right-style:").append(fzc > -1 ? "solid" : "none").append(";");
		sb.append("}");
		sb.append(name).append(" .zsfzleft .zswidgetpanel{");
		sb.append("top:").append(-th).append("px;");
		sb.append("}");
		
		sb.append(name).append(" .zsfzcorner{");
		sb.append("border-right-style:").append(fzc > -1 ? "solid" : "none")
				.append(";");
		sb.append("}");

		// TODO transparent border mode
		boolean transparentBorder = false;
		if (transparentBorder) {
			sb.append(name).append(" .zscell {");
			sb.append("border-right-color: transparent;");
			sb.append("border-bottom-color: transparent;");
//			if (isIE) {
//				/** for IE6 **/
//				String color_to_transparent = "tomato";
//				sb.append("_border-color:" + color_to_transparent + ";");
//				sb.append("_filter:chroma(color=" + color_to_transparent + ");");
//			}
			sb.append("}");
		}

		List<HeaderPositionInfo> infos = colHelper.getInfos();
		for (HeaderPositionInfo info : infos) {
			boolean hidden = info.hidden;
			int index = info.index;
			int width = hidden ? 0 : info.size;
			int cid = info.id;

			celltextwidth = width - 2 * cp;

			// bug 1989680
			if (celltextwidth < 0)
				celltextwidth = 0;

			cellwidth = width;

			if (width <= 0) {
				sb.append(name).append(" .zsw").append(cid).append("{");
				sb.append("display:none;");
				sb.append("}");

			} else {
				sb.append(name).append(" .zsw").append(cid).append("{");
				sb.append("width:").append(cellwidth).append("px;");
				sb.append("}");

				sb.append(name).append(" .zswi").append(cid).append("{");
				sb.append("width:").append(celltextwidth).append("px;");
				sb.append("}");
			}
		}

		infos = rowHelper.getInfos();
		for (HeaderPositionInfo info : infos) {
			boolean hidden = info.hidden;
			int index = info.index;
			int height = hidden ? 0 : info.size;
			int cid = info.id;
			cellheight = height;

			if (height <= 0) {
				
				// ZSS-330, ZSS-382: using "height: 0" and don't use "display: none", latter one cause merge cell to chaos
				sb.append(name).append(" .zsh").append(cid).append("{");
				sb.append("height:0px;");
				sb.append("}");

				// ZSS-500: re-overwrite overflow to hidden when row hidden 
				sb.append(name).append(" .zshi").append(cid).append("{");
				sb.append("height:0px;");
				sb.append("border-bottom-width:0px;");
				sb.append("overflow:hidden;");
				sb.append("}");

				sb.append(name).append(" .zslh").append(cid).append("{");
				sb.append("height:0px;");
				sb.append("line-height:0px;");
				sb.append("border-bottom-width:0px;");
				sb.append("}");

			} else {
				sb.append(name).append(" .zsh").append(cid).append("{");
				sb.append("height:").append(height).append("px;");
				sb.append("}");

				sb.append(name).append(" .zshi").append(cid).append("{");
				sb.append("height:").append(cellheight).append("px;");
				sb.append("border-bottom-width:1px;");
				sb.append("}");

				sb.append(name).append(" .zslh").append(cid).append("{");
				sb.append("height:").append(height).append("px;");
				sb.append("line-height:").append(height).append("px;");
				sb.append("border-bottom-width:1px;");
				sb.append("}");

			}
		}
		//TODO: seems no need
		sb.append(".zs_header{}");// for indicating add new rule before this

		// merge size;
		List ranges = mmhelper.getRanges();
		Iterator iter = ranges.iterator();
		final int defaultSize = colHelper.getDefaultSize();
		final int defaultRowSize = rowHelper.getDefaultSize();

		while (iter.hasNext()) {
			MergedRect block = (MergedRect) iter.next();
			int left = block.getColumn();
			int right = block.getLastColumn();
			int width = 0;
			for (int i = left; i <= right; i++) {
				final HeaderPositionInfo info = colHelper.getInfo(i);
				if (info != null) {
					final boolean hidden = info.hidden;
					final int colSize = hidden ? 0 : info.size;
					width += colSize;
				} else {
					width += defaultSize ;
				}
			}
			int top = block.getRow();
			int bottom = block.getLastRow();
			int height = 0;
			for (int i = top; i <= bottom; i++) {
				final HeaderPositionInfo info = rowHelper.getInfo(i);
				if (info != null) {
					final boolean hidden = info.hidden;
					final int rowSize = hidden ? 0 : info.size;
					height += rowSize;
				} else {
					height += defaultRowSize ;
				}
			}

			if (width <= 0 || height <= 0) { //total hidden
				sb.append(name).append(" .zsmerge").append(block.getId()).append("{");
				sb.append("display:none;");
				sb.append("}");

				sb.append(name).append(" .zsmerge").append(block.getId());
				sb.append(" .zscelltxt").append("{");
				sb.append("display:none;");
				sb.append("}");
			} else {
				celltextwidth = width - 2 * cp;
				int celltextheight = height;
	
				cellwidth = width;
				cellheight = height;
				
				sb.append(name).append(" .zsmerge").append(block.getId()).append("{");
				sb.append("width:").append(cellwidth).append("px;");
				sb.append("height:").append(cellheight).append("px;");
				sb.append("display:inline-block;"); // ZSS-330, ZSS-382:  the left-top cell must display
				sb.append("border-bottom-width: 1px;"); // re-apply bottom border for grid line; Or grid line will be missed if row was hidden
				sb.append("}");

				sb.append(name).append(" .zsmerge").append(block.getId());
				sb.append(" .zscelltxt").append("{");
				sb.append("width:").append(celltextwidth).append("px;");
				sb.append("height:").append(celltextheight).append("px;");
				sb.append("}");
			}
		}

		//gridline
//		if (sheet.isDisplayGridlines()) {
//			sb.append(name).append(" .zscell")
//			.append("{border-bottom-color:#FFFFFF;border-right-color:#FFFFFF;}");
//		}
		sb.append(name).append(" .zs_indicator_" + getSelectedSheetId() + "{}");// for indicating the css is load ready

		return sb.toString();
	}

	/**
	 * Returns the encoded URL for the dynamic generated content, or empty the
	 * component doesn't belong to any desktop.
	 */
	private static String getDynamicMediaURI(AbstractComponent comp, int version, String name, String format) {
		final Desktop desktop = comp.getDesktop();
		if (desktop == null)
			return ""; // no avail at client

		final StringBuffer sb = new StringBuffer(64).append('/');
		Strings.encode(sb, version);
		if (name != null || format != null) {
			sb.append('/');
			boolean bExtRequired = true;
			if (name != null && name.length() != 0) {
				sb.append(name.replace('\\', '/'));
				bExtRequired = name.lastIndexOf('.') < 0;
			} else {
				sb.append(comp.getUuid());
			}
			if (bExtRequired && format != null)
				sb.append('.').append(format);
		}

		return desktop.getDynamicMediaURI(comp, sb.toString()); // already
		// encoded
	}

	private void cleanSelectedSheet() {
		if(_selectedSheet==null){
			return;
		}
		deleteSelfEditorFocus();
		
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).onSheetClean(_selectedSheet);
		}
		
//		_loadedRect.set(-1, -1, -1, -1);
		_selectionArea.setArea(0, 0, 0, 0);
		_focusArea.setArea(0, 0, 0, 0);
		
		_selectedSheet = null;
	}

	private void afterSheetSelected() {
		//Dennis, shouldn't post event in component by a server side operation call
//		org.zkoss.zk.ui.event.Events.postEvent(new Event(Events.ON_SHEET_SELECT, this));
		
		//load widgets
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).onSheetSelected(_selectedSheet);
		}
		//setup gridline
		setDisplayGridlines(_selectedSheet.getViewInfo().isDisplayGridlines());
		setProtectSheet(_selectedSheet.isProtected());
		
		//register collaborated focus
		CellRef cf = getCellFocus();
		moveSelfEditorFocus(getSelectedSheetId(),cf.getRow(),cf.getColumn());
		
		refreshToolbarDisabled();
		refreshAllowedOptions();
		updateUnlockInfo();
	}
	
	public String getSelectedSheetName() {
		SSheet sheet = getSelectedSSheet();
		return sheet==null?null:sheet.getSheetName();
	}
	
	private void addChartWidget(SSheet sheet, SChart chart) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		//load widgets
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).addChartWidget(sheet, chart);
		}
	}

	private void addPictureWidget(SSheet sheet, SPicture picture) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		//load widgets
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).addPictureWidget(sheet, picture);
		}
	}

	private void deletePictureWidget(SSheet sheet, String pictureId) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		//load widgets
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).deletePictureWidget(sheet, pictureId);
		}
	}
	
	private void updatePictureWidget(SSheet sheet, SPicture picture) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		//load widgets
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).updatePictureWidget(sheet, picture);
		}
	}

	private void deleteChartWidget(SSheet sheet, String chartId) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		//load widgets
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).deleteChartWidget(sheet, chartId);
		}
	}

	private void updateChartWidget(SSheet sheet, SChart chart) {
		if (!getSelectedSSheet().equals(sheet)){
			releaseClientCache(sheet.getId());
			return;
		}
		//load widgets
		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).updateChartWidget(sheet, chart);
		}
	}

	private void clearHeaderSizeHelper(boolean row, boolean col) {
		if (row)
			removeAttribute(ROW_SIZE_HELPER_KEY);
		if (col)
			removeAttribute(COLUMN_SIZE_HELPER_KEY);
	}

	private static String getSizeHelperStr(HeaderPositionHelper helper) {
		List<HeaderPositionInfo> infos = helper.getInfos();
		StringBuffer csc = new StringBuffer();
		for(HeaderPositionInfo info : infos) {
			if (csc.length() > 0)
				csc.append(",");
			csc.append(info.index).append(",")
				.append(info.size).append(",")
				.append(info.id).append(",")
				.append(info.hidden).append(",")
				.append(info.isCustom());
		}
		return csc.toString();
	}

	static private String getRectStr(AreaRef rect) {
		StringBuffer sb = new StringBuffer();
		sb.append(rect.getColumn()).append(",").append(rect.getRow()).append(",")
				.append(rect.getLastColumn()).append(",").append(rect.getLastRow());
		return sb.toString();
	}

	private void doInvalidate() {
		//TODO: reset here ?
//		_loadedRect.set(-1, -1, -1, -1);
		
		SSheet sheet = getSelectedSSheet();

		clearHeaderSizeHelper(true, true);
		// remove this, beacuse invalidate will cause client side rebuild,
		// i must reinitial size helper since there are maybe some customized is
		// from client.
		// System.out.println(">>>>>>>>>>>remove this");
		// removeAttribute(MERGE_MATRIX_KEY);//TODO remove this, for insert
		// column test only

		_custColId = new SequenceId(-1, 2);
		_custRowId = new SequenceId(-1, 2);

		this.getWidgetHandler().invaliate();

		List list = loadWidgetLoaders();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			((WidgetLoader) list.get(i)).invalidate();
		}
	}

	/**
	 * load widget loader
	 */
	/* package */List<WidgetLoader> loadWidgetLoaders() {
		if (_widgetLoaders != null)
			return _widgetLoaders;
		_widgetLoaders = new ArrayList<WidgetLoader>();
		final String loaderclzs = (String) Library.getProperty(WIDGET_LOADERS);
		if (loaderclzs != null) {
			try {
				String[] clzs = loaderclzs.split(",");
				WidgetLoader wl;
				for (int i = 0; i < clzs.length; i++) {
					clzs[i] = clzs[i].trim();
					if ("".equals(clzs[i]))
						continue;
					wl = (WidgetLoader) Classes.newInstance(clzs[i], null, null);
					wl.init(this);
					_widgetLoaders.add(wl);
				}
			} catch (Exception x) {
				throw new UiException(x);
			}
		}
		return _widgetLoaders;
	}

	/**
	 * Sets the {@link AuxAction} disabled
	 * 
	 * @param disabled
	 * @param action
	 */
	public void disableUserAction(AuxAction action, boolean disabled) {
		boolean changed = false;
		if (disabled && !_actionDisabled.contains(action)) {
			_actionDisabled.add(action);
			changed = true;
		} else if (!disabled && _actionDisabled.contains(action)) {
			_actionDisabled.remove(action);
			changed = true;
		}
		if (changed) {
			refreshToolbarDisabled();
		}
	}
	
	private List<String> convertToDisabledActionJSON(Set<String> supported) {
		ArrayList<String> disd = new ArrayList<String>();
		String act;
		for(AuxAction ua:AuxAction.values()){
			act = ua.toString();
			if(_actionDisabled.contains(ua) 
					|| supported==null || !supported.contains(act)){
				disd.add(act);
			}
		}
		return disd;
	}

	/**
	 * get widget handler
	 */
	private WidgetHandler getWidgetHandler() {
		if (_widgetHandler == null) {
			_widgetHandler = newWidgetHandler();
		}
		return _widgetHandler;
	}

	/**
	 * new widget handler
	 * 
	 * @return
	 */
	private WidgetHandler newWidgetHandler() {
		final String handlerclz = (String) Library.getProperty(WIDGET_HANDLER_CLS);
		if (handlerclz != null) {
			try {
				_widgetHandler = (WidgetHandler) Classes.newInstance(handlerclz, null, null);
				_widgetHandler.init(this);
			} catch (Exception x) {
				throw new UiException(x);
			}
		} else {
			_widgetHandler = new VoidWidgetHandler();
			_widgetHandler.init(this);
		}
		return _widgetHandler;
	}

	/**
	 * Add widget to the {@link WidgetHandler} of this spreadsheet,
	 * 
	 * @param widget a widget
	 * @return true if success to add a widget
	 */
	private boolean addWidget(Widget widget) {
		return getWidgetHandler().addWidget(widget);
	}

	/**
	 * Remove widget from the {@link WidgetHandler} of this spreadsheet,
	 * 
	 * @param widget
	 * @return true if success to remove a widget
	 */
	private boolean removeWidget(Widget widget) {
		return getWidgetHandler().removeWidget(widget);
	}

	private void processStartEditing(String token, StartEditingEvent event, String editingType) {
		if (!event.isCancel()) {
			Object val;
			// ZSS-536 Don't need to care about condition : clienttxt == null
			// only use server-side value when server-side modify it, otherwise, use client-side
			final boolean useEditValue = event.isEditingSet();
			if (useEditValue) {
				val = event.getEditingValue();
			} else {
				val = event.getClientValue();
			}
			processStartEditing0(token,event.getSheet(), event.getRow(), event
					.getColumn(), val, useEditValue, editingType);
		} else {
			processCancelEditing0(token, event.getSheet(), event.getRow(),
					event.getColumn(), false, editingType);
		}
	}

	private void processStopEditing(String token, StopEditingEvent event, String editingType) {
		if (!event.isCancel()) {
			processStopEditing0(token, event.getSheet(), event.getRow(), event.getColumn(), event.getEditingValue(), editingType);
		} else
			processCancelEditing0(token, event.getSheet(), event.getRow(), event.getColumn(), false, editingType);
	}
	
	private void showFormulaErrorThenRetry(IllegalFormulaException ex, final String token, final Sheet sheet, final int rowIdx,final int colIdx, final Object value, final String editingType) {
		String title = Labels.getLabel("zss.msg.warn_title");
		String msg = Labels.getLabel("zss.msg.formula_error",new Object[]{ex.getMessage()});
		Messagebox.show(msg, title, Messagebox.OK, Messagebox.EXCLAMATION, new EventListener() {
			public void onEvent(Event evt) {
				Spreadsheet.this.processRetryEditing0(token, sheet, rowIdx, colIdx, value, editingType);
			}
		});
	}
	
	private void showInvalidateModelOpErrorThenRetry(InvalidModelOpException ex, final String token, final Sheet sheet, final int rowIdx,final int colIdx, final Object value, final String editingType) {
		String title = Labels.getLabel("zss.msg.warn_title");
		String msg = Labels.getLabel("zss.msg.invalidate_model_op_error",new Object[]{ex.getMessage()});
		Messagebox.show(msg, title, Messagebox.OK, Messagebox.EXCLAMATION, new EventListener() {
			public void onEvent(Event evt) {
				Spreadsheet.this.processRetryEditing0(token, sheet, rowIdx, colIdx, value, editingType);
			}
		});
	}
	

	// a local flag indicates that skip the validation and force this editing (ZSS-351) 
	private boolean forceStopEditing0 = false; 
	
	private void processStopEditing0(final String token, final Sheet sheet, final int rowIdx, final int colIdx, final Object value, final String editingType) {
		try {
			
			String editText = value == null ? "" : value.toString();
			// ZSS-351: use force flag to skip the validation when user want force this editing
			if (!forceStopEditing0 && !getDataValidationHandler().validate(sheet, rowIdx, colIdx, editText,
				//callback
				new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						final String eventname = event.getName();
						if (Messagebox.ON_CANCEL.equals(eventname)) { //cancel
							Spreadsheet.this.processCancelEditing0(token, sheet, rowIdx, colIdx, true, editingType); //skipMove
						} else if (Messagebox.ON_OK.equals(eventname)) { //ok
							try {
								forceStopEditing0 = true;
								Spreadsheet.this.processStopEditing0(token, sheet, rowIdx, colIdx, value, editingType);
							} finally {
								forceStopEditing0 = false;
							}
						} else { //retry
							Spreadsheet.this.processRetryEditing0(token, sheet, rowIdx, colIdx, value, editingType);
						}
					}
				}
			)) {
				return;
			}else{
				UndoableActionManager uam = getUndoableActionManager();
				uam.doAction(new CellEditTextAction(Labels.getLabel("zss.undo.editText"),sheet,rowIdx,colIdx,rowIdx,colIdx,editText));
			}

			//JSONObj result = new JSONObj();
			JSONObject result = new JSONObject();
			result.put("r", rowIdx);
			result.put("c", colIdx);
			result.put("type", "stopedit");
			result.put("val", "");
			result.put("et", editingType);

			smartUpdate("dataUpdateStop", new Object[] { token,	getSheetUuid(sheet), result});
		} catch (RuntimeException x) {
			if (x instanceof IllegalFormulaException) {
				showFormulaErrorThenRetry((IllegalFormulaException)x, token, sheet, rowIdx, colIdx, value, editingType);
			} else if (x instanceof InvalidModelOpException){
				showInvalidateModelOpErrorThenRetry((InvalidModelOpException)x, token, sheet, rowIdx, colIdx, value, editingType);
			} else {
				processCancelEditing0(token, sheet, rowIdx, colIdx, false, editingType);
				throw x;
			}
		}
	}
	
	private static String getSheetUuid(Sheet sheet){
		return sheet.getInternalSheet().getId();
	}

	private void processStartEditing0(String token, Sheet sheet, int row, int col, Object value, boolean useEditValue, String editingType) {
		try {
			JSONObject result = new JSONObject();
			result.put("r", row);
			result.put("c", col);
			result.put("type", "startedit");
			result.put("val", value == null ? "" : value.toString());
			result.put("et", editingType);
			if (useEditValue) { //shall use edit value from server
				result.put("server", true); 
			}
			smartUpdate("dataUpdateStart", new Object[] { token, getSheetUuid(sheet), result});
		} catch (RuntimeException x) {
			processCancelEditing0(token, sheet, row, col, false, editingType);
			throw x;
		}
	}

	private void processCancelEditing0(String token, Sheet sheet, int row, int col, boolean skipMove, String editingType) {
		JSONObject result = new JSONObject();
		result.put("r", row);
		result.put("c", col);
		result.put("type", "canceledit");
		result.put("val", "");
		result.put("sk", skipMove);
		result.put("et", editingType);
		smartUpdate("dataUpdateCancel", new Object[] { token, getSheetUuid(sheet), result});
	}

	private void processRetryEditing0(String token, Sheet sheet, int row, int col, Object value, String editingType) {
		try {
			processCancelEditing0(token, sheet, row, col, true, editingType);
			JSONObject result = new JSONObject();
			result.put("r", row);
			result.put("c", col);
			result.put("type", "retryedit");
			result.put("val", value);
			result.put("et", editingType);
			smartUpdate("dataUpdateRetry", new Object[] { "", getSheetUuid(sheet), result});
		} catch (RuntimeException x) {
			processCancelEditing0(token, sheet, row, col, false, editingType);
			throw x;
		}
	}

	public boolean insertBefore(Component newChild, Component refChild) {
		// not all child can insert into spreadsheet a child want to insert to spreadsheet must provide some speaciall
		// attribute
		if (newChild.getAttribute(SpreadsheetCtrl.CHILD_PASSING_KEY) != null) {
			return super.insertBefore(newChild, refChild);
		} else {
			throw new UiException("Unsupported child for Spreadsheet: " + newChild);
		}
	}
	/**
	 * push the current cell state of selection region and selected sheet
	 */
/*	public void pushCellState(){
		stateManager.pushCellState();
	}
*/	
	/**
	 * push current cell state of selected sheet and specified region rect
	 * @param rect
	 */
/*	public void pushCellState(Rect rect){
		stateManager.pushCellState(rect);
	}
*/	
	/**
	 * push current cell state in specified sheets and rects
	 * @param sheets
	 * @param rectArray
	 */
/*	public void pushCellState(Sheet[] sheets, Rect[] rectArray){
		stateManager.pushCellState(sheets, rectArray);
	}
*/	
	/**
	 * push certain state to redostack
	 * @param iState
	 */
/*	public void pushRedoState(IState iState){
		stateManager.pushRedoState(iState);
	}
*/	
	/**
	 * 
	 * @return the top IState in the undostack
	 */
/*	public IState peekUndoState(){
		return stateManager.peekUndoStack();
	}
*/	
	/**
	 * push the current state into undostack before change the col/row header size
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
/*	public void pushRowColSizeState(int left, int top, int right, int bottom){	
		stateManager.pushRowColSizeState(left, top, right, bottom);
	}
*/	
	/**
	 * push the current state into undostack before insert the row/col operation
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	
/*	public void pushInsertRowColState(int left, int top, int right, int bottom){
		stateManager.pushInsertRowColState(left, top, right, bottom);
	}
*/	
	/**
	 * push the current state into undostack before delete row/col operation
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
/*	public void pushDeleteRowColState(int left, int top, int right, int bottom){
		stateManager.pushDeleteRowColState(left, top, right, bottom);
	}
*/	
	/**
	 * undo operation and notify other editors
	 */
/*	public void undo(){
		stateManager.undo();
	}
*/	
	/**
	 * redo, and notify other editors 
	 */
/*	public void redo(){
		stateManager.redo();
	}
*/	
	/**
	 * return the undo/redo state manager
	 */
/*	public StateManager getStateManager(){
		return this.stateManager;
	}
*/	
	/**
	 * Remove editor's focus on specified name
	 */
	public void removeEditorFocus(String id){
		response("removeEditorFocus" + _focusId.next(), new AuInvoke((Component)this, "removeEditorFocus", id));
		_editorFocuses.remove(id);
	}
	
	private void removeFriendFocus(String id){
		//use same au response. in client side there is always editor focus
		if(_friendFocuses.remove(id)!=null){
			response("removeEditorFocus" + _focusId.next(), new AuInvoke((Component)this, "removeEditorFocus", id));
			//don't remove firendFocuses color cache, it might be back when switch sheet, only remove when sync firend focus 
		}
	}
	
	/**
	 *  Add and move other editor's focus
	 */
	public void moveEditorFocus(String id, String name, String color, int row ,int col){
		if (_selfEditorFocus != null && !_selfEditorFocus.getId().equals(id)) {
			response("moveEditorFocus" + _focusId.next(), new AuInvoke((Component)this, "moveEditorFocus", new String[]{id, name, color,""+row,""+col}));
			_editorFocuses.put(id, new Focus(id, name, color, getSelectedSheetName(), row, col, null));
		}
	}
	
	private void addOrMoveFriendFocus(String id, String name, String color, String sheetId, int row ,int col){
		if (_selfEditorFocus != null && !_selfEditorFocus.getId().equals(id) && getSelectedSSheet()!=null) {
			if(sheetId!=null && sheetId.equals(getSelectedSheetId())){
				response("moveEditorFocus" + _focusId.next(), new AuInvoke((Component)this, "moveEditorFocus", new String[]{id, name, color,""+row,""+col}));
				_friendFocuses.put(id, new FriendFocus(id, name, color, sheetId, row, col));
			}else{
				removeFriendFocus(id);
			}
		}
	}	
	
	private static class FriendFocus extends Focus{
		public FriendFocus(String id, String name, String color,
				String sheetId, int row, int col) {
			super(id, name, color, sheetId, row, col,null);
		}
	}
	
	private void syncFriendFocus() {
		if (_book != null) {
			final Set<Object> bookFocuses;
			final Set<String> keep = new HashSet<String>();
			final Set<String> inbook = new HashSet<String>();
			
			bookFocuses = getFriendFocusHelper().getAllFocus();
			
			String sheetid = getSelectedSheetId();
			for(Object f:bookFocuses){
				if(!(f instanceof Focus) || f.equals(_selfEditorFocus)){
					continue;
				}
				Focus focus = (Focus)f;
				String id = focus.getId();
				inbook.add(id);
				if(focus.getSheetId().equals(sheetid)){
					if(!_friendFocuses.containsKey(id)){
						//same sheet, but not in friend focus, add back
						addOrMoveFriendFocus(id, focus.getName(), focus.getColor(), focus.getSheetId(), focus.getRow(), focus.getColumn());
					}
					keep.add(id);//same sheet, keep it in friend focus 
				}else if(_friendFocuses.containsKey(id)){
					//different sheet and in firend, remove it
					removeFriendFocus(id);
				}
			}
			//remove other friend focus that not in book focus
			for(String fid:new HashSet<String>(_friendFocuses.keySet())){
				if(keep.contains(fid)) continue;
				removeFriendFocus(fid);				
			}
		}
	}
	
	/**
	 * Set focus name of this spreadsheet.
	 * @param name focus name that show on other Spreadsheet
	 */
	public void setUserName(String name) {
		if(!Objects.equals(_userName, name)){
			_userName = name;
			if (_selfEditorFocus != null) {
				_selfEditorFocus.setName(name);
				//TODO UPDATE self and relative
				moveSelfEditorFocus(_selfEditorFocus.getSheetId(), _selfEditorFocus.getRow(), _selfEditorFocus.getColumn());
			}	
		}
	}

	//sync friend focus position after some size change
	private void syncFriendFocusPosition(int left, int top, int right, int bottom) {
		int row = -1, col = -1;
		for(Focus focus : new ArrayList<Focus>(_friendFocuses.values())) {//avoid co-modify-exception
			row=focus.getRow();
			col=focus.getColumn();
			if(col>=left && col<=right && row>=top  && row<=bottom) {
				this.addOrMoveFriendFocus(focus.getId(), focus.getName(), focus.getColor(), focus.getSheetId(),row, col);
			}
		}
	}
	
//	/**
//	 * update/invalidate all focus/selection/hightlight to align with cell border
//	 */
//	public void updateFocus(int left, int top, int right, int bottom){
//		int row,col,sL,sT,sR,sB,hL,hT,hR,hB;
//		row=col=sL=sT=sR=sB=hL=hT=hR=hB=-1;
//		Position pos = this.getCellFocus();
//		if(pos!=null){
//			row=pos.getRow();
//			col=pos.getColumn();
//			response("updateSelfFocus", new AuInvoke((Component)this,"updateSelfFocus", new String[]{""+row,""+col}));
//		}
//		Rect rect = this.getSelection();
//		if(rect!=null){
//			sL=rect.getLeft();
//			sT=rect.getTop();
//			sR=rect.getRight();
//			sB=rect.getBottom();	
//			response("updateSelfSelection", new AuInvoke((Component)this,"updateSelfSelection", new String[]{""+sL,""+sT,""+sR,""+sB}));
//		}
//		
//		rect=this.getHighlight();
//		if(rect!=null){
//			hL=rect.getLeft();
//			hT=rect.getTop();
//			hR=rect.getRight();
//			hB=rect.getBottom();
//			
//			response("updateSelfHightlight", new AuInvoke((Component)this,"updateSelfHighlight", new String[]{""+hL,""+hT,""+hR,""+hB}));
//			
//		}
//	}
	
	/**
	 * @param sheet
	 */
	//it will be call when delete sheet 
/*	public void cleanRelatedState(Sheet sheet){
		stateManager.cleanRelatedState(sheet);
	}
*/

	static {
		//ZSS-220 Can't get correct selection if I didn't listen to onCellSelection
		//onCellSelection should be a important event.
		addClientEvent(Spreadsheet.class, Events.ON_CELL_SELECTION,	CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_CELL_SELECTION_UPDATE, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_CELL_FOUCS, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		//can't ignore duplicate, for different header resize
		addClientEvent(Spreadsheet.class, Events.ON_HEADER_UPDATE, CE_IMPORTANT | CE_NON_DEFERRABLE);
		addClientEvent(Spreadsheet.class, Events.ON_SHEET_SELECT, CE_IMPORTANT | CE_DUPLICATE_IGNORE | CE_NON_DEFERRABLE);
		
		addClientEvent(Spreadsheet.class, Events.ON_CELL_CLICK, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_CELL_RIGHT_CLICK, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_CELL_DOUBLE_CLICK, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_HEADER_CLICK, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_HEADER_RIGHT_CLICK,	CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_HEADER_DOUBLE_CLICK, CE_DUPLICATE_IGNORE);
		
		
		addClientEvent(Spreadsheet.class, Events.ON_EDITBOX_EDITING, 0);
		
		//ZSS-325 Cannot copy an area of cells from Excel to Spreadsheet
		addClientEvent(Spreadsheet.class, Events.ON_START_EDITING, CE_IMPORTANT | CE_NON_DEFERRABLE);
		addClientEvent(Spreadsheet.class, Events.ON_STOP_EDITING, CE_IMPORTANT | CE_NON_DEFERRABLE);
		
		addClientEvent(Spreadsheet.class, Events.ON_CELL_HYPERLINK, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_CELL_FILTER, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_CELL_VALIDATOR, CE_DUPLICATE_IGNORE);
		
		
		addClientEvent(Spreadsheet.class, Events.ON_CTRL_KEY, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_AUX_ACTION, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_WIDGET_CTRL_KEY, CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, Events.ON_WIDGET_UPDATE, CE_DUPLICATE_IGNORE);

		//Event dispatcher
		addClientEvent(Spreadsheet.class, InnerEvts.ON_ZSS_CELL_MOUSE, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, InnerEvts.ON_ZSS_HEADER_MOUSE, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		
		//Inner
		addClientEvent(Spreadsheet.class, InnerEvts.ON_ZSS_CELL_FETCH, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, InnerEvts.ON_ZSS_FETCH_ACTIVE_RANGE, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		addClientEvent(Spreadsheet.class, InnerEvts.ON_ZSS_SYNC_BLOCK, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
		
		
		//TODO Dennis, why need this and is importnat?Review
//		addClientEvent(Spreadsheet.class, org.zkoss.zk.ui.event.Events.ON_BLUR,	CE_IMPORTANT | CE_DUPLICATE_IGNORE);//
	}



	// super//
	/**
	 * Processes an AU request. It is invoked internally.
	 * 
	 * <p>
	 * Default: in addition to what are handled by {@link XulElement#service},
	 * it also handles onChange.
	 */
	public void service(AuRequest request, boolean everError) {
		final String cmd = request.getCommand();

		if (Events.ON_CELL_HYPERLINK.equals(cmd)) {
			final CellHyperlinkEvent evt = CellHyperlinkEvent.getHyperlinkEvent(request);
			if (evt != null) {
				org.zkoss.zk.ui.event.Events.postEvent(evt);
			}
			return;
		}
		
		Command command = InnerEvts.getCommand(cmd);
		if (command != null) {
			command.process(request);
			return;
		}
		
		super.service(request, everError);
	}
	
	public void smartUpdate(String attr, Object value) {
		super.smartUpdate(attr, value);
	}
	
	public void smartUpdate(String attr, Object value, boolean append) {
		super.smartUpdate(attr, value, append);
	}
	
	public void response(String key, AuResponse response) {
		super.response(key, response);
	}
	

	@Override
	public void afterCompose() {		
	}
	
	@Override
	public void onPageAttached(Page newpage, Page oldpage){
		super.onPageAttached(newpage, oldpage);
		String uuid = getUuid();
		Desktop desktop = Executions.getCurrent().getDesktop();
		if(_bookCleaner==null){
			_bookCleaner = new BookCleaner(uuid);
			desktop.addListener(_bookCleaner);
		}
	}
	
	@Override
	public void onPageDetached(Page page){
		super.onPageDetached(page);
		Book book = getBook();
		if(book!=null){
			String scope = book.getShareScope();
			if(isCleanBookAutomatically() && !Strings.isEmpty(scope)){
				setBook(null);
			}
		}
		Desktop desktop = Executions.getCurrent().getDesktop();
		if(_bookCleaner!=null){
			desktop.removeListener(_bookCleaner);
			_bookCleaner = null;
		}
	}
	
	/**
	 * @return true if should clean book automatically when component detached
	 * @since 3.0.0
	 */
	protected boolean isCleanBookAutomatically(){
		return true;
	}
	
	public class HelperContainer<T> {
		HashMap<String, T> helpers = new HashMap<String, T>();
		
		public T getHelper(String sheetId) {
			return helpers.get(sheetId);
		}
		
		public void putHelper(String sheetId, T helper) {
			helpers.put(sheetId, helper);
		}
	}

	//new wrapped API since 3.0.0
	
	/**
	 * Returns the book model of this Spreadsheet. If you call this method at
	 * first time and the book has not assigned by {@link #setBook(Book)}, this
	 * will create a new model depends on src;
	 * 
	 * @return the book model of this spread sheet.
	 */
	public Book getBook(){
		SBook book = getSBook();
		return book==null?null:new BookImpl(new SimpleRef<SBook>(book));
	}
	
	/**
	 * Sets the book data model of this spread sheet.
	 * 
	 * @param book the book data model.
	 */
	public void setBook(Book book) {
		setSBook((SBook)(book==null?null:((BookImpl)book).getNative()));
	}
	
	/**
	 * Gets the selected sheet, the default selected sheet is first sheet.
	 * @return #{@link Sheet}
	 */
	public Sheet getSelectedSheet(){
		SSheet sheet = getSelectedSSheet();
		return sheet==null?null:new SheetImpl(new SimpleRef<SBook>(sheet.getBook()),new SimpleRef<SSheet>(sheet));
	}
	
	/**
	 * Returns the maximum visible number of columns of this spreadsheet. You can assign
	 * new numbers by calling {@link #setMaxVisibleColumns(int)}.
	 * 
	 * @return the maximum visible number of columns 
	 * @since 3.0.0
	 */
	public int getMaxVisibleColumns() {
		return _maxColumns;
	}

	/**
	 * Returns the maximum visible number of rows of this spreadsheet. You can assign
	 * new number by calling {@link #setMaxVisibleRows(int)}.
	 * 
	 * @return the maximum visible number of rows.
	 * @since 3.0.0
	 */
	public int getMaxVisibleRows() {
		return _maxRows;
	}
	
	/**
	 * Sets the maximum visible number of columns of this spreadsheet. For example, if you
	 * set this parameter to 40, it will allow showing only column 0 to column 39. the minimal value of
	 * max number of columns must large than 0;
	 * 
	 * @param maxcols  the maximum visible number of columns 
	 * @since 3.0.0
	 */
	public void setMaxVisibleColumns(int maxcols){
		if (maxcols < 1) {
			throw new UiException("maxcolumn must be greater than 0: " + maxcols);
		}

		if (_maxColumns != maxcols) {
			_maxColumns = maxcols;
//
//			if (_colFreeze >= _maxColumns) {
//				_colFreeze = _maxColumns - 1;
//			}
			
			smartUpdate("maxColumns", getMaxVisibleColumns());
			// 20140514, RaymondChao: unlock info records until max visible column,
			// so it needs to update when max visible column changed.
			if (getSelectedSheet() != null) {
				updateUnlockInfo();
			}
		}
	}
	
	/**
	 * Sets the maximum visible number of rows of this spreadsheet. For example, if you set
	 * this parameter to 40, it will allow showing only row 0 to 39. The minimal value of max number of rows
	 * must large than 0; <br/>
	 * Default : 20.
	 * 
	 * @param maxrows  the maximum visible number of rows
	 * @since 3.0.0
	 */
	public void setMaxVisibleRows(int maxrows){
		if (maxrows < 1) {
			throw new UiException("maxrow must be greater than 0: " + maxrows);
		}

		if (_maxRows != maxrows) {
			_maxRows = maxrows;
//			if (_rowFreeze >= _maxRows) {
//				_rowFreeze = _maxRows - 1;
//			}
			smartUpdate("maxRows", getMaxVisibleRows());
		}
	}
	
	/**
	 * Gets the importer that import the file in the specified src (
	 * {@link #getSrc}) to {@link Book} data model. The default importer is
	 * excel importer.
	 * 
	 * @return the importer
	 */
	public Importer getImporter(){
		return this.getSImporter()==null?null:new ImporterImpl(getSImporter());
	}
	
	/**
	 * Sets the importer for import the book data model from a specified src.
	 * 
	 * @param importer the importer to import a spread sheet file from a document
	 * format (e.g. an Excel file) by the specified src (@link
	 * #setSrc(). The default importer is excel importer
	 */
	public void setImporter(Importer importer){
		setSImporter(importer==null?null:((ImporterImpl)importer).getNative());
	}
	
	
	
	private void refreshToolbarDisabled(){
		if(!isInvalidated()){
			smartUpdate("actionDisabled",
					convertToDisabledActionJSON(getUserActionManagerCtrl()
							.getSupportedUserAction(getSelectedSheet())));
		}
	}
	
	private void refreshAllowedOptions(){
		final SheetProtection sheetProtection = Ranges.range(getSelectedSheet()).getSheetProtection();
		smartUpdate("allowSelectLockedCells", sheetProtection.isSelectLockedCellsAllowed());
		smartUpdate("allowSelectUnlockedCells", sheetProtection.isSelectUnlockedCellsAllowed());
		smartUpdate("allowFormatCells", sheetProtection.isFormatCellsAllowed());
		smartUpdate("allowFormatColumns", sheetProtection.isFormatColumnsAllowed());
		smartUpdate("allowFormatRows", sheetProtection.isFormatRowsAllowed());
		smartUpdate("allowAutoFilter", sheetProtection.isAutoFilterAllowed());
		smartUpdate("objectEditable", sheetProtection.isObjectsEditable());
	}
	
	/*
	 *  Update unlock info including rows, cols and cells. 
	 */
	private void updateUnlockInfo() {
		final SheetProtection sheetProtection = Ranges.range(getSelectedSheet()).getSheetProtection();
		if (!sheetProtection.isSelectLockedCellsAllowed() &&
				sheetProtection.isSelectUnlockedCellsAllowed()) {
			JSONObject unlockInfo = createUnlockInfo(getSelectedSheet());
			smartUpdate("unlockInfo", unlockInfo);
		}
	}
	
	/*
	 *  Unlock info:
	 *  chs: unlock columns
	 *  rhs: unlock rows
	 *  cs: unlock cells
	 */
	private JSONObject createUnlockInfo(Sheet sheet) {
		final SSheet ssheet = sheet.getInternalSheet();
		final int startColumn = ssheet.getStartColumnIndex(),
				// 20140513, RaymondChao: use getMaxVisibleColumns() instead of ssheet.getEndColumnIndex()
				// for better performance, because getEndColumnIndex() could be 16384.
				endColumn = getMaxVisibleColumns(),
				startRow = ssheet.getStartRowIndex(),
				endRow = ssheet.getEndRowIndex();
				
		JSONObject attrs = new JSONObject();
		JSONArray rows = new JSONArray(),
				columns = new JSONArray(),
				cells = new JSONArray();
		
		if (startColumn != -1) {
			// 20140513, RaymondChao: group unlocked columns to improve the performance.
			int start = -1;
			for (int i = startColumn; i <= endColumn; i++) {
				if (ssheet.getColumn(i).getCellStyle().isLocked()) {
					if (start != -1) {
						columns.add(createUnlockGroup(start, i - 1));
						start = -1;
					}
				} else {
					if (start == -1) {
						start = i;
					}
				}
			}
			if (start != -1) {
				columns.add(createUnlockGroup(start, endColumn));
			}
		}
		
		if (startRow != -1) {
			
			for (int i = startRow; i <= endRow; i++) {
				if (!ssheet.getRow(i).getCellStyle().isLocked())
					rows.add(i);

				final int firstColumn = sheet.getFirstColumn(i),
					lastColumn = sheet.getLastColumn(i);
				if (firstColumn == -1)
					continue;
				int start = -1;
				JSONObject row = new JSONObject();
				JSONArray data = new JSONArray();
				row.put("i", i);
				// 20140513, RaymondChao: group unlocked cells to improve the performance.
				for (int j = firstColumn; j <= lastColumn; j++) {
					if (Ranges.range(sheet, i, j).getCellStyle().isLocked()) {
						if (start != -1) {
							data.add(createUnlockGroup(start, j - 1));
							start = -1;
						}
					} else {
						if (start == -1) {
							start = j;
						}
					}
				}
				if (start != -1) {
					data.add(createUnlockGroup(start, lastColumn));
				}
				row.put("data", data);
				cells.add(row);
			}
		}
		attrs.put("chs", columns);
		attrs.put("rhs", rows);
		attrs.put("cs", cells);
		return attrs;
	}
	
	/*
	 *  Unlocked group info:
	 *  start: start index of unlocked group
	 *  end: end index of unlocked group
	 */
	private JSONObject createUnlockGroup(int start, int end) {
		JSONObject group = new JSONObject();
		group.put("start", start);
		group.put("end", end);
		return group;
	}
	

	private CellDisplayLoader getCellDisplayLoader() {
		if(_cellDisplayLoader==null){
			String cls = (String) Library.getProperty(CELL_DISPLAY_LOADER_CLS);
			if (cls != null) {
				try {
					_cellDisplayLoader = (CellDisplayLoader) Classes.newInstance(cls, null, null);
				} catch (Exception x) {
					throw new UiException(x);
				}
			} else {
				_cellDisplayLoader = new SimpleCellDisplayLoader();
			}
		}
		return _cellDisplayLoader;
	}
	
	private DataValidationHandler getDataValidationHandler() {
		if(_dataValidationHandler==null){
			String cls = (String) Library.getProperty(DATA_VALIDATION_HANDLER_CLS);
			if (cls != null) {
				try {
					_dataValidationHandler = (DataValidationHandler) Classes.newInstance(cls, null, null);
				} catch (Exception x) {
					throw new UiException(x);
				}
			} else {
				_dataValidationHandler = new DummyDataValidationHandler();
			}
		}
		return _dataValidationHandler;
	}
	
	private FreezeInfoLoader getFreezeInfoLoader() {
		if(_freezeInfoLoader==null){
			String cls = (String) Library.getProperty(FREEZE_INFO_LOCADER_CLS);
			if (cls != null) {
				try {
					_freezeInfoLoader = (FreezeInfoLoader) Classes.newInstance(cls, null, null);
				} catch (Exception x) {
					throw new UiException(x);
				}
			} else {
				_freezeInfoLoader = new DummyFreezeInfoLoader();
			}
		}
		return _freezeInfoLoader;
	}
	
	public UndoableActionManager getUndoableActionManager(){
		if(_undoableActionManager==null){
			String cls = (String) getAttribute(UNDOABLE_ACTION_MANAGER_CLS,true);
			
			if(cls==null){
				cls = (String) Library.getProperty(UNDOABLE_ACTION_MANAGER_CLS);
			}
			if (cls != null) {
				try {
					_undoableActionManager = (UndoableActionManager) Classes.newInstance(cls, null, null);
				} catch (Exception x) {
					throw new UiException(x);
				}
			} else {
				_undoableActionManager = new DummyUndoableActionManager();
			}
			_undoableActionManager.bind(this);
		}
		return _undoableActionManager;
	}
	
	private void clearUndoableActionManager(){
		if(_undoableActionManager!=null){
			_undoableActionManager.clear();
		}
	}
	
	/**
	 * clear book after desktop cleanup, to clean listener that register to a
	 * book
	 **/
	private static class BookCleaner implements DesktopCleanup,Serializable{
		
		private String _ssid;
		public BookCleaner(String ssid){
			this._ssid = ssid;
		}
		
		@Override
		public void cleanup(Desktop desktop) throws Exception {
			Component comp = desktop.getComponentByUuid(_ssid);
			if(comp instanceof Spreadsheet){
				try{
					((Spreadsheet)comp).releaseBook();
				}catch(Exception x){}//eat
			}
		}
	}
	
}
