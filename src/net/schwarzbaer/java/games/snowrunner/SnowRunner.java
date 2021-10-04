package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.java.games.snowrunner.Data.AddonCategories;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.DataTables.CombinedTableTabPaneTextAreaPanel;
import net.schwarzbaer.java.games.snowrunner.DataTables.TruckTableModel;
import net.schwarzbaer.java.games.snowrunner.MapTypes.StringVectorMap;
import net.schwarzbaer.java.games.snowrunner.MapTypes.VectorMap;
import net.schwarzbaer.java.games.snowrunner.MapTypes.VectorMapMap;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.ListenerSource;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.ListenerSourceParent;
import net.schwarzbaer.system.DateTimeFormatter;
import net.schwarzbaer.system.Settings;

public class SnowRunner {

	static final AppSettings settings = new AppSettings();

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new SnowRunner().initialize();
	}


	private Data data;
	private HashMap<String,String> truckToDLCAssignments;
	private SaveGameData saveGameData;
	private SaveGame selectedSaveGame;
	private final StandardMainWindow mainWindow;
	private final JMenu languageMenu;
	private final Controllers controllers;
	private final RawDataPanel rawDataPanel;
	private final JMenuItem miSGValuesSorted;
	private final JMenuItem miSGValuesOriginal;
	private final JMenu selectedSaveGameMenu;
	
	SnowRunner() {
		data = null;
		truckToDLCAssignments = null;
		saveGameData = null;
		selectedSaveGame = null;
		
		mainWindow = new StandardMainWindow("SnowRunner Tool");
		controllers = new Controllers();
		
		rawDataPanel = new RawDataPanel(mainWindow, controllers);
		
		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.addTab("Trucks"          , new TrucksPanel(mainWindow,controllers));
		contentPane.addTab("Trucks II"       , new TrucksPanel2(mainWindow, controllers));
		contentPane.addTab("Wheels"          , DataTables.SimplifiedTablePanel.create(new DataTables.WheelsTableModel     (controllers)));
		contentPane.addTab("DLCs"            , DataTables.SimplifiedTablePanel.create(new DataTables.DLCTableModel        (controllers)));
		contentPane.addTab("Trailers"        , DataTables.SimplifiedTablePanel.create(new DataTables.TrailersTableModel   (controllers,true)));
		contentPane.addTab("Truck Addons"    , new TruckAddonsTablePanel(controllers));
		contentPane.addTab("Engines"         , DataTables.SimplifiedTablePanel.create(new DataTables.EnginesTableModel    (controllers,true)));
		contentPane.addTab("Gearboxes"       , DataTables.SimplifiedTablePanel.create(new DataTables.GearboxesTableModel  (controllers,true)));
		contentPane.addTab("Suspensions"     , DataTables.SimplifiedTablePanel.create(new DataTables.SuspensionsTableModel(controllers,true)));
		contentPane.addTab("Winches"         , DataTables.SimplifiedTablePanel.create(new DataTables.WinchesTableModel    (controllers,true)));
		contentPane.addTab("Addon Categories", DataTables.SimplifiedTablePanel.create(new DataTables.AddonCategoriesTableModel(controllers)));
		contentPane.addTab("Raw Data", rawDataPanel);
		
		
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = menuBar.add(new JMenu("File"));
		fileMenu.add(createMenuItem("Reload \"initial.pak\"", true, e->{
			boolean changed = reloadInitialPAK();
			if (changed) updateAfterDataChange();
		}));
		fileMenu.add(createMenuItem("Reset application settings", true, e->{
			for (AppSettings.ValueKey key:AppSettings.ValueKey.values())
				settings.remove(key);
		}));
		//fileMenu.add(createMenuItem("Test XMLTemplateStructure", true, e->{
		//	boolean changed = testXMLTemplateStructure();
		//	if (changed) updateAfterDataChange();
		//}));
		
		languageMenu = menuBar.add(new JMenu("Language"));
		
		ButtonGroup bg = new ButtonGroup();
		miSGValuesSorted   = createCheckBoxMenuItem("Show Values Sorted by Name"   ,  rawDataPanel.isShowingSaveGameDataSorted(), bg, false, e->rawDataPanel.showSaveGameDataSorted(true ));
		miSGValuesOriginal = createCheckBoxMenuItem("Show Values in Original Order", !rawDataPanel.isShowingSaveGameDataSorted(), bg, false, e->rawDataPanel.showSaveGameDataSorted(false));
		
		JMenu saveGameDataMenu = menuBar.add(new JMenu("SaveGame Data"));
		saveGameDataMenu.add(createMenuItem("Reload SaveGame Data", true, e->reloadSaveGameData()));
		saveGameDataMenu.addSeparator();
		saveGameDataMenu.add(selectedSaveGameMenu = new JMenu("Selected SaveGame"));
		saveGameDataMenu.addSeparator();
		saveGameDataMenu.add(miSGValuesSorted  );
		saveGameDataMenu.add(miSGValuesOriginal);
		selectedSaveGameMenu.setEnabled(false);
		
		JMenu testingMenu = menuBar.add(new JMenu("Testing"));
		testingMenu.add(createMenuItem("Show Event Listeners", true, e->{
			controllers.showListeners();
		}));
		
		mainWindow.setIconImagesFromResource("/images/AppIcons/AppIcon","016.png","024.png","032.png","040.png","048.png","056.png","064.png","128.png","256.png");
		mainWindow.startGUI(contentPane, menuBar);
		
		if (settings.isSet(AppSettings.ValueGroup.WindowPos )) mainWindow.setLocation(settings.getWindowPos ());
		if (settings.isSet(AppSettings.ValueGroup.WindowSize)) mainWindow.setSize    (settings.getWindowSize());
		
		mainWindow.addComponentListener(new ComponentListener() {
			@Override public void componentShown  (ComponentEvent e) {}
			@Override public void componentHidden (ComponentEvent e) {}
			@Override public void componentResized(ComponentEvent e) { settings.setWindowSize( mainWindow.getSize() ); }
			@Override public void componentMoved  (ComponentEvent e) { settings.setWindowPos ( mainWindow.getLocation() ); }
		});
	}

	interface TruckToDLCAssignmentListener {
		void setTruckToDLCAssignments(HashMap<String, String> assignments);
		void updateAfterAssignmentsChange();
	}
	
	private void initialize() {
		truckToDLCAssignments = TruckAssignToDLCDialog.loadStoredData();
		controllers.truckToDLCAssignmentListeners.setTruckToDLCAssignments(truckToDLCAssignments);
		
		if (reloadInitialPAK  ()) updateAfterDataChange();
		if (reloadSaveGameData()) updateAfterSaveGameChange();
		
	}

	private boolean reloadSaveGameData() {
		File saveGameFolder = getSaveGameFolder();
		if (saveGameFolder==null) return false;
		
		System.out.printf("Read Data from SaveGame Folder \"%s\" ...%n", saveGameFolder.getAbsolutePath());
		saveGameData = new SaveGameData(saveGameFolder);
		saveGameData.readData();
		System.out.printf("... done%n");
		
		Vector<String> indexStrs = new Vector<>(saveGameData.saveGames.keySet());
		indexStrs.sort(null);
		
		selectedSaveGame = null;
		String selectedSaveGameIndexStr = settings.getString(AppSettings.ValueKey.SelectedSaveGame, null);
		if (selectedSaveGameIndexStr!=null) {
			selectedSaveGame = saveGameData.saveGames.get(selectedSaveGameIndexStr);
		}
		if (selectedSaveGame==null && !saveGameData.saveGames.isEmpty()) {
			if (indexStrs.size()==1) {
				selectedSaveGameIndexStr = indexStrs.get(0);
			} else {
				String[] values = indexStrs.stream().map(this::getSaveGameLabel).toArray(String[]::new);
				Object selection = JOptionPane.showInputDialog(mainWindow, "Select a SaveGame: ", "Select SaveGame", JOptionPane.QUESTION_MESSAGE, null, values, values[0]);
				int index = Arrays.asList(values).indexOf(selection);
				selectedSaveGameIndexStr = index<0 ? null : indexStrs.get(index);
			}
			if (selectedSaveGameIndexStr!=null)
				selectedSaveGame = saveGameData.saveGames.get(selectedSaveGameIndexStr);
			if (selectedSaveGame!=null)
				settings.putString(AppSettings.ValueKey.SelectedSaveGame, selectedSaveGameIndexStr);
		}
		
		ButtonGroup bg = new ButtonGroup();
		selectedSaveGameMenu.removeAll();
		selectedSaveGameMenu.setEnabled(true);
		for (String indexStr : indexStrs) {
			boolean isSelected = selectedSaveGame!=null && indexStr.equals(selectedSaveGame.indexStr);
			selectedSaveGameMenu.add(createCheckBoxMenuItem(getSaveGameLabel(indexStr), isSelected, bg, true, e->{
				selectedSaveGame = saveGameData.saveGames.get(indexStr);
				if (selectedSaveGame!=null)
					settings.putString(AppSettings.ValueKey.SelectedSaveGame, indexStr);
				else
					settings.remove(AppSettings.ValueKey.SelectedSaveGame);
				updateAfterSaveGameChange();
			}));
		}
		
		rawDataPanel.setData(saveGameData);
		
		miSGValuesSorted  .setEnabled(true);
		miSGValuesOriginal.setEnabled(true);
		return true;
	}

	private String getSaveGameLabel(String indexStr) {
		SaveGame saveGame = saveGameData==null || saveGameData.saveGames==null ? null : saveGameData.saveGames.get(indexStr);
		String mode     = saveGame==null ? "??" : saveGame.isHardMode ? "HardMode" : "NormalMode";
		String saveTime = saveGame==null ? "??" : new DateTimeFormatter().getTimeStr(saveGame.saveTime, false, true, false, true, false);
		return String.format("SaveGame %s (%s, %s)", indexStr, mode, saveTime);
	}

	private boolean reloadInitialPAK() {
		return testXMLTemplateStructure();
//		File initialPAK = getInitialPAK();
//		if (initialPAK!=null) {
//			Data newData = Data.readInitialPAK(initialPAK);
//			if (newData!=null) {
//				data = newData;
//				return true;
//			}
//		}
//		return false;
	}

	private boolean testXMLTemplateStructure() {
		File initialPAK = getInitialPAK();
		if (initialPAK!=null) {
			Data newData = ProgressDialog.runWithProgressDialogRV(mainWindow, String.format("Read \"%s\"", initialPAK.getAbsolutePath()), 600, pd->{
				Data localData = testXMLTemplateStructure(pd,initialPAK);
				if (Thread.currentThread().isInterrupted()) return null;
				return localData;
			});
			//Data newData = testXMLTemplateStructure(initialPAK);
			if (newData!=null) {
				data = newData;
				return true;
			}
		}
		return false;
	}
	
	private Data testXMLTemplateStructure(ProgressDialog pd, File initialPAK) {
		setTask(pd, "Read XMLTemplateStructure");
		System.out.printf("XMLTemplateStructure.");
		XMLTemplateStructure structure = XMLTemplateStructure.readPAK(initialPAK,mainWindow);
		if (structure==null) return null;
		if (Thread.currentThread().isInterrupted()) return null;
		setTask(pd, "Parse Data from XMLTemplateStructure");
		System.out.printf("Parse Data from XMLTemplateStructure ...%n");
		Data data = new Data(structure);
		System.out.printf("... done%n");
		if (Thread.currentThread().isInterrupted()) return null;
		return data;
	}


	static void setTask(ProgressDialog pd, String taskTitle) {
		SwingUtilities.invokeLater(()->{
			pd.setTaskTitle(taskTitle);
			pd.setIndeterminate(true);
		});
	}
	 
	private File getSaveGameFolder() {
		// c:\\Program Files (x86)\\Steam\\userdata\\<Account>\\1465360\\remote
		File saveGameFolder = settings.getFile(AppSettings.ValueKey.SaveGameFolder, null);
		if (saveGameFolder != null && saveGameFolder.isDirectory())
			return saveGameFolder;
		
		File steamFolder = new File("C:\\Program Files (x86)\\Steam");
		if (!steamFolder.isDirectory())
			steamFolder = new File("C:\\Program Files\\Steam");
		if (!steamFolder.isDirectory())
			return getSaveGameFolder_askUser();
		
		File userdataFolder = new File(steamFolder,"userdata");
		if (!userdataFolder.isDirectory())
			return getSaveGameFolder_askUser();
		
		File[] accountFolders = userdataFolder.listFiles(file->{
			String name = file.getName();
			return file.isDirectory() && !name.equals(".") && !name.equals("..");
		});
		
		saveGameFolder = null;
		for (File accountFolder : accountFolders) {
			saveGameFolder = new File(accountFolder,"1465360\\remote");
			if (saveGameFolder.isDirectory()) break;
			saveGameFolder = null;
		}
		if (saveGameFolder==null)
			return getSaveGameFolder_askUser();
		
		settings.putFile(AppSettings.ValueKey.SaveGameFolder, saveGameFolder);
		return saveGameFolder;
	}
	
	private File getSaveGameFolder_askUser() {
		JFileChooser fc = new JFileChooser("./");
		fc.setDialogTitle("Select SaveGame Folder");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
		fc.setMultiSelectionEnabled(false);
		if (fc.showOpenDialog(mainWindow)!=JFileChooser.APPROVE_OPTION)
			return null;
		
		File saveGameFolder = fc.getSelectedFile();
		settings.putFile(AppSettings.ValueKey.SaveGameFolder, saveGameFolder);
		return saveGameFolder;
	}

	private File getInitialPAK() {
		File initialPAK = settings.getFile(AppSettings.ValueKey.InitialPAK, null);
		if (initialPAK==null || !initialPAK.isFile()) {
			DataSelectDialog dlg = new DataSelectDialog(mainWindow);
			initialPAK = dlg.showDialog();
			if (initialPAK == null || !initialPAK.isFile())
				return null;
			settings.putFile(AppSettings.ValueKey.InitialPAK, initialPAK);
		}
		return initialPAK;
	}

	interface SaveGameListener {
		void setSaveGame(SaveGame saveGame);
	}
	
	private void updateAfterSaveGameChange() {
		controllers.saveGameListeners.setSaveGame(selectedSaveGame);
	}

	interface DataReceiver {
		void setData(Data data);
	}
	
	private void updateAfterDataChange() {
		controllers.dataReceivers.setData(data);
		
		Vector<String> langs = new Vector<>(data.languages.keySet());
		langs.sort(null);
		String currentLangID = settings.getString(AppSettings.ValueKey.Language, null);
		
		ButtonGroup bg = new ButtonGroup();
		languageMenu.removeAll();
		for (String langID:langs)
			languageMenu.add(createCheckBoxMenuItem(langID, langID.equals(currentLangID), bg, true, e->setLanguage(langID)));
		
		setLanguage(currentLangID);
	}
	
	interface LanguageListener {
		void setLanguage(Language language);
	}
	
	private void setLanguage(String langID) {
		Language language = langID==null ? null : data.languages.get(langID);
		
		if (language!=null)
			settings.putString(AppSettings.ValueKey.Language, langID);
		else
			settings.remove(AppSettings.ValueKey.Language);
		
		/*
		// DEBUG
		if (language!=null) {
			for (String country : new String[] {"US","RU"}) {
				System.out.printf("Country: %s%n", country);
				for (int region=1; region<7; region++) {
					String regionName_ID = String.format("%s_%02d", country, region);
					String regionName = language.dictionary.get(regionName_ID);
					System.out.printf("   Region[%d]:  %s  <%s>%n", region, regionName, regionName_ID);
					for (int map=1; map<6; map++) {
						String mapName_ID = String.format("%s_%02d_%02d_NAME", country, region, map);
						String mapName = language.dictionary.get(mapName_ID);
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d_NAME", country, region, map);
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("%s_%02d_%02d_NEW_NAME", country, region, map);
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d", country, region, map).toLowerCase();
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d", country, region, map).toLowerCase()+"_NAME";
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName!=null)
							System.out.printf("      Map[%d,%d]:  %s  <%s>%n", region, map, mapName, mapName_ID);
					}
				}
			}
		}
		// DEBUG
		*/
		
		controllers.languageListeners.setLanguage(language);
	}

	static String solveStringID(String strID, Language language) {
		if (strID==null) return null;
		return solveStringID(strID, language, "<"+strID+">");
	}
	static String solveStringID(String strID, Language language, String defaultStr) {
		if (strID==null) strID = defaultStr;
		String str = null;
		if (language!=null) str = language.dictionary.get(strID);
		if (str==null) str = defaultStr;
		return str;
	}
	
	static String getReducedString(String str, int maxLength) {
		if (str==null) return null;
		if (str.length() > maxLength-4)
			return str.substring(0,maxLength-4)+" ...";
		return str;
	}
	
	static void removeRedundantStrs(Vector<String> strs, boolean sort) {
		for (int i=0; i<strs.size(); i++) {
			String name = strs.get(i);
			int nextEqual = strs.indexOf(name, i+1);
			while (nextEqual>=0) {
				strs.remove(nextEqual);
				nextEqual = strs.indexOf(name, i+1);
			}
		}
		if (sort) strs.sort(null);
	}

	static String[] removeRedundantStrs(String[] strs, boolean sort) {
		Vector<String> vec = new Vector<>(Arrays.asList(strs));
		removeRedundantStrs(vec, sort);
		return vec.toArray(new String[vec.size()]);
	}

	static String selectNonNull(String... strings) {
		for (String str : strings)
			if (str!=null)
				return str;
		return null;
	}

	static JCheckBoxMenuItem createCheckBoxMenuItem(String title, boolean isSelected, ButtonGroup bg, boolean isEnabled, ActionListener al) {
		JCheckBoxMenuItem comp = new JCheckBoxMenuItem(title,isSelected);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		if (bg!=null) bg.add(comp);
		return comp;
	}

	static JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	
	static JButton createButton(String title, boolean isEnabled, ActionListener al) {
		JButton comp = new JButton(title);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	static <AC> JButton createButton(String title, boolean isEnabled, Disabler<AC> disabler, AC ac, ActionListener al) {
		JButton comp = createButton(title, isEnabled, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}

	static JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, ActionListener al) {
		JRadioButton comp = new JRadioButton(title);
		if (bg!=null) bg.add(comp);
		comp.setEnabled(isEnabled);
		comp.setSelected(isSelected);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	static <AC> JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, Disabler<AC> disabler, AC ac, ActionListener al) {
		JRadioButton comp = createRadioButton(title, bg, isEnabled, isSelected, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}
	
	static <AC> JLabel createLabel(String text, Disabler<AC> disabler, AC ac) {
		JLabel comp = new JLabel(text);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}

	static String getTruckLabel(Truck truck, Language language) {
		return getTruckLabel(truck, language, true);
	}
	static String getTruckLabel(Truck truck, Language language, boolean addInternalDLC ) {
		if (truck==null)
			return "<null>";
		
		String truckName = SnowRunner.solveStringID(truck.name_StringID, language, "<"+truck.id+">");
		
		if (addInternalDLC && truck.dlcName!=null)
			truckName = String.format("%s [%s]", truckName, truck.dlcName);
		
		return truckName;
	}

	static String[] getTruckAddonNames(String[] idList, Function<String,String> getName_StringID, Language language) {
		if (getName_StringID==null)
			return idList;
		
		String[] namesArr = Arrays.stream(idList).map(id->getNameFromID(id, getName_StringID, language)).toArray(String[]::new);
		return removeRedundantStrs(namesArr, true);
	}

	static Function<String,String> createGetNameFunction(HashMap<String, TruckAddon> truckAddons, HashMap<String, Trailer> trailers) {
		if (truckAddons==null && trailers==null)
			return null;
		
		return id -> {
			if (id == null)
				return "<null>";
			
			String name_StringID = null;
			
			if (truckAddons!=null && name_StringID==null) {
				TruckAddon truckAddon = truckAddons.get(id);
				if (truckAddon != null) name_StringID = truckAddon.name_StringID;
			}
			if (trailers!=null && name_StringID==null) {
				Trailer trailer = trailers.get(id);
				if (trailer != null) name_StringID = trailer.name_StringID;
			}
			
			return name_StringID;
		};
	}

	static String getNameFromID(String id, Function<String,String> getName_StringID, Language language) {
		if (getName_StringID==null)
			throw new IllegalArgumentException();
		if (id == null) return "<null>";
		String name_StringID = getName_StringID.apply(id);
		return solveStringID(name_StringID, language, String.format("<%s>", id));
	}

	static String joinTruckNames(Vector<Truck> list, Language language) {
		return String.join(", ", (Iterable<String>)()->list.stream().map(truck->SnowRunner.solveStringID(truck.name_StringID, language, "<"+truck.id+">")).sorted().iterator());
	}

	static String joinRequiredAddonsToString(String[][] requiredAddons, String indent) {
		return joinRequiredAddonsToString(requiredAddons, null, null, indent);
	}

	static String joinRequiredAddonsToString(String[][] requiredAddons, Function<String,String> getName_StringID, Language language, String indent) {
		if (requiredAddons==null || requiredAddons.length==0) return null;
		Iterable<String> it = ()->Arrays.stream(requiredAddons).map(list->String.join("  OR  ", getTruckAddonNames(list,getName_StringID,language))).iterator();
		String orGroupIndent = "  ";
		return indent+orGroupIndent+String.join(String.format("%n%1$sAND%n%1$s"+orGroupIndent, indent), it);
	}

	static String joinRequiredAddonsToString_OneLine(String[][] requiredAddons) {
		if (requiredAddons==null || requiredAddons.length==0) return null;
		
		Iterable<String> it = ()->Arrays.stream(requiredAddons).map(list->{
			String str = String.join(" OR ", Arrays.asList(list));
			if (list.length==1) return str;
			return String.format("(%s)", str);
		}).iterator();
		
		String str = String.join(" AND ", it);
		if (requiredAddons.length==1) return str;
		return String.format("(%s)", str);
	}
	
	public static String joinAddonIDs(String[] strs) {
		if (strs==null) return "<null>";
		if (strs.length==0) return "[]";
		if (strs.length==1) return strs[0];
		return Arrays.toString(strs);
	}


	static final Comparator<String> CATEGORY_ORDER = Comparator.<String>comparingInt(SnowRunner::getCategoryOrderIndex).thenComparing(Comparator.naturalOrder());
	static final List<String> CATEGORY_ORDER_LIST = Arrays.asList("Trailers", "engine", "gearbox", "suspension", "winch", "awd", "diff_lock", "frame_addons");
	static int getCategoryOrderIndex(String category) {
		//return "frame_addons".equals(category) ? 0 : 1;
		int pos = CATEGORY_ORDER_LIST.indexOf(category);
		if (pos<0) return CATEGORY_ORDER_LIST.size();
		return pos;
	}

	private static class TruckAddonsTablePanel extends CombinedTableTabPaneTextAreaPanel implements ListenerSource, ListenerSourceParent {
		private static final String CONTROLLERS_CHILDLIST_TABTABLEMODELS = "TabTableModels";

		private static final long serialVersionUID = 7841445317301513175L;
		
		private final Controllers controllers;
		private final Vector<Tab> tabs;
		private Data data;
		private Language language;

		TruckAddonsTablePanel(Controllers controllers) {
			this.controllers = controllers;
			this.language = null;
			this.data = null;
			this.tabs = new Vector<>();
			
			this.controllers.languageListeners.add(this,language->{
				this.language = language;
				updateTabTitles();
			});
			this.controllers.dataReceivers.add(this,data->{
				this.data = data;
				rebuildTabPanels();
			});
		}

		private void updateTabTitles() {
			AddonCategories addonCategories = data==null ? null : data.addonCategories;
			for (int i=0; i<tabs.size(); i++)
				setTabTitle(i, tabs.get(i).getTabTitle(addonCategories, language));
			
		}

		private void rebuildTabPanels() {
			removeAllTabs();
			tabs.clear();
			controllers.removeListenersOfVolatileChildren(this, CONTROLLERS_CHILDLIST_TABTABLEMODELS);
			
			if (data==null) return;
			
			StringVectorMap<TruckAddon> truckAddons = new StringVectorMap<>();
			for (TruckAddon addon : data.truckAddons.values())
				truckAddons.add(addon.getCategory(), addon);
			
			Tab allTab = new Tab(null, data.truckAddons.size());
			tabs.add(allTab);
			
			String title = allTab.getTabTitle(data.addonCategories, language);
			DataTables.TruckAddonsTableModel tableModel = new DataTables.TruckAddonsTableModel(controllers,false);
			controllers.addVolatileChild(this, CONTROLLERS_CHILDLIST_TABTABLEMODELS, tableModel);
			addTab(title, tableModel);
			tableModel.setData(data.truckAddons.values());
			
			Vector<String> categories = new Vector<>(truckAddons.keySet());
			categories.sort(CATEGORY_ORDER);
			
			for (String category : categories) {
				Vector<TruckAddon> list = truckAddons.get(category);
				
				Tab tab = new Tab(category, list.size());
				tabs.add(tab);
				
				title = tab.getTabTitle(data.addonCategories, language);
				tableModel = new DataTables.TruckAddonsTableModel(controllers,false);
				controllers.addVolatileChild(this, CONTROLLERS_CHILDLIST_TABTABLEMODELS, tableModel);
				addTab(title, tableModel);
				tableModel.setData(list);
			}
		}
		
		private static class Tab {

			final String category;
			final int size;

			Tab(String category, int size) {
				this.category = category;
				this.size = size;
			}
			
			String getTabTitle(AddonCategories addonCategories, Language language) {
				String categoryLabel = category==null ? "All" : addonCategories==null ? AddonCategories.getCategoryLabel(category) : addonCategories.getCategoryLabel(category, language);
				return String.format("%s [%d]", categoryLabel, size);
			}
		}
	}

	private static class TrucksPanel2 extends JSplitPane implements ListenerSourceParent, ListenerSource {
		private static final long serialVersionUID = 6564351588107715699L;
		
		private final StandardMainWindow mainWindow;
		private final Controllers controllers;

		TrucksPanel2(StandardMainWindow mainWindow, Controllers controllers) {
			super(JSplitPane.VERTICAL_SPLIT, true);
			setResizeWeight(1);
			
			this.mainWindow = mainWindow;
			this.controllers = controllers;
			
			TruckPanel truckPanel = new TruckPanel(this.mainWindow, this.controllers);
			this.controllers.addChild(this,truckPanel);
			JTabbedPane truckPanelTabbedPane = truckPanel.createTabbedPane();
			truckPanelTabbedPane.setBorder(BorderFactory.createTitledBorder("Selected Truck"));

			TruckTableModel truckTableModel = new DataTables.TruckTableModel(controllers);
			JComponent truckTableScrollPane = DataTables.SimplifiedTablePanel.create( truckTableModel, rowIndex -> truckPanel.setTruck(truckTableModel.getRow(rowIndex)) );
			
			setTopComponent(truckTableScrollPane);
			setBottomComponent(truckPanelTabbedPane);
			
			
//			JTable table = new JTable();
//			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//			JScrollPane scrollPane = new JScrollPane(table);
//			//scrollPane.setPreferredSize(new Dimension(400,100));
//			
//			table.setModel(tableModel);
//			tableModel.setTable(table);
//			tableModel.setColumnWidths(table);
//			
//			SimplifiedRowSorter rowSorter = new SimplifiedRowSorter(tableModel);
//			table.setRowSorter(rowSorter);
//			
//			ContextMenu contextMenu = new ContextMenu();
//			contextMenu.addTo(table);
//			contextMenu.add(createMenuItem("Reset Row Order",true,e->{
//				rowSorter.resetSortOrder();
//				table.repaint();
//			}));
//			contextMenu.add(createMenuItem("Show Column Widths", true, e->{
//				System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
//			}));
//			
//			setTopComponent(comp);
		}
		
	}

	private static class TrucksPanel extends JSplitPane implements ListenerSourceParent, ListenerSource {
		private static final long serialVersionUID = 7004081774916835136L;
		
		private final JList<Truck> truckList;
		private final Controllers controllers;
		private final StandardMainWindow mainWindow;
		private HashMap<String, String> truckToDLCAssignments;
		
		TrucksPanel(StandardMainWindow mainWindow, Controllers controllers) {
			super(JSplitPane.HORIZONTAL_SPLIT);
			this.mainWindow = mainWindow;
			this.controllers = controllers;
			this.truckToDLCAssignments = null;
			setResizeWeight(0);
			
			TruckPanel truckPanel = new TruckPanel(this.mainWindow, this.controllers);
			this.controllers.addChild(this,truckPanel);
			JSplitPane truckPanelSplitPane = truckPanel.createSplitPane();
			truckPanelSplitPane.setBorder(BorderFactory.createTitledBorder("Selected Truck"));
			
			JScrollPane truckListScrollPane = new JScrollPane(truckList = new JList<>());
			truckListScrollPane.setBorder(BorderFactory.createTitledBorder("All Trucks in Game"));
			truckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			truckList.addListSelectionListener(e->truckPanel.setTruck(truckList.getSelectedValue()));
			this.controllers.dataReceivers.add(this,data -> {
				Vector<Truck> items = new Vector<>(data.trucks.values());
				items.sort(Comparator.<Truck,String>comparing(Truck->Truck.id));
				truckList.setListData(items);
			});
			
			TruckListContextMenu truckListContextMenu = new TruckListContextMenu();
			this.controllers.languageListeners.add(this,truckListContextMenu);
			
			TruckListCellRenderer truckListCellRenderer = new TruckListCellRenderer(truckList);
			truckList.setCellRenderer(truckListCellRenderer);
			this.controllers.languageListeners.add(this,truckListCellRenderer);
			this.controllers.saveGameListeners.add(this,truckListCellRenderer);
			
			setLeftComponent(truckListScrollPane);
			setRightComponent(truckPanelSplitPane);
			
			this.controllers.truckToDLCAssignmentListeners.add(this,new TruckToDLCAssignmentListener() {
				@Override public void updateAfterAssignmentsChange() {}
				@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
					TrucksPanel.this.truckToDLCAssignments = truckToDLCAssignments;
				}
			});
		}
	
		private class TruckListContextMenu extends ContextMenu implements LanguageListener {
			private static final long serialVersionUID = 2917583352980234668L;
			
			private int clickedIndex;
			private Truck clickedItem;
			private Language language;
		
			TruckListContextMenu() {
				clickedIndex = -1;
				clickedItem = null;
				language = null;
				
				addTo(truckList);
				
				JMenuItem miAssignToDLC = add(createMenuItem("Assign truck to an official DLC", true, e->{
					if (clickedItem==null) return;
					TruckAssignToDLCDialog dlg = new TruckAssignToDLCDialog(mainWindow, clickedItem, language, truckToDLCAssignments);
					boolean assignmentsChanged = dlg.showDialog();
					if (assignmentsChanged)
						controllers.truckToDLCAssignmentListeners.updateAfterAssignmentsChange();
				}));
				
				addContextMenuInvokeListener((comp, x, y) -> {
					clickedIndex = truckList.locationToIndex(new Point(x,y));
					clickedItem = clickedIndex<0 ? null : truckList.getModel().getElementAt(clickedIndex);
					
					miAssignToDLC.setEnabled(clickedItem!=null);
					
					miAssignToDLC.setText(
						clickedItem==null
						? "Assign truck to an official DLC"
						: String.format("Assign \"%s\" to an official DLC", getTruckLabel(clickedItem,language))
					);
				});
				
			}
			
			@Override public void setLanguage(Language language) {
				this.language = language;
			}
		}
	
		private static class TruckListCellRenderer implements ListCellRenderer<Truck>, LanguageListener, SaveGameListener {
			
			private static final Color COLOR_FG_DLCTRUCK   = new Color(0x0070FF);
			private static final Color COLOR_FG_OWNEDTRUCK = new Color(0x00AB00);
			private final Tables.LabelRendererComponent rendererComp;
			private final JList<Truck> truckList;
			private Language language;
			private SaveGame saveGame;
		
			TruckListCellRenderer(JList<Truck> truckList) {
				this.truckList = truckList;
				rendererComp = new Tables.LabelRendererComponent();
				language = null;
				saveGame = null;
			}
			
			@Override public void setLanguage(Language language) {
				this.language = language;
				truckList.repaint();
			}
			
			@Override public void setSaveGame(SaveGame saveGame) {
				this.saveGame = saveGame;
				truckList.repaint();
			}

			@Override
			public Component getListCellRendererComponent( JList<? extends Truck> list, Truck value, int index, boolean isSelected, boolean cellHasFocus) {
				rendererComp.configureAsListCellRendererComponent(list, null, getTruckLabel(value, language), index, isSelected, cellHasFocus, null, ()->getForeground(value));
				return rendererComp;
			}
		
			private Color getForeground(Truck truck) {
				if (truck!=null) {
					if (saveGame!=null && saveGame.ownedTrucks!=null && saveGame.ownedTrucks.containsKey(truck.id))
						return COLOR_FG_OWNEDTRUCK;
					if (truck.dlcName!=null)
						return COLOR_FG_DLCTRUCK;
				}
				return null;
			}
		
		}
		
	}

	static class Controllers {
		
		interface ListenerSource {}
		interface ListenerSourceParent {}
		
		final LanguageListenerController languageListeners;
		final DataReceiverController dataReceivers;
		final SaveGameListenerController saveGameListeners;
		final TruckToDLCAssignmentListenerController truckToDLCAssignmentListeners;
		final VectorMap<ListenerSourceParent, ListenerSource> childrenOfSources;
		final VectorMapMap<ListenerSourceParent, String, ListenerSource> volatileChildrenOfSources;
		
		Controllers() {
			languageListeners             = new LanguageListenerController();
			dataReceivers                 = new DataReceiverController();
			saveGameListeners             = new SaveGameListenerController();
			truckToDLCAssignmentListeners = new TruckToDLCAssignmentListenerController();
			childrenOfSources = new VectorMap<>();
			volatileChildrenOfSources = new VectorMapMap<>();
		}
		
		void showListeners() {
			String indent = "    ";
			
			System.out.printf("Current State of Listeners:%n");
			languageListeners            .showListeners(indent, "LanguageListeners"            );
			dataReceivers                .showListeners(indent, "DataReceivers"                );
			saveGameListeners            .showListeners(indent, "SaveGameListeners"            );
			truckToDLCAssignmentListeners.showListeners(indent, "TruckToDLCAssignmentListeners");
			
			System.out.printf("%sChild Relations: [%d]%n", indent, childrenOfSources.size());
			childrenOfSources.forEach((parent,children)->{
				System.out.printf("%1$s%1$s%2$s: [%3$d]%n", indent, parent, children.size());
				children.forEach(l->{
					System.out.printf("%1$s%1$s%1$s%2$s%n", indent, l);
				});
			});
			
			System.out.printf("%sVolatile Children: [%d]%n", indent, volatileChildrenOfSources.size());
			volatileChildrenOfSources.forEach((parent,map2)->{
				System.out.printf("%1$s%1$s%2$s: [%3$d]%n", indent, parent, map2.size());
				map2.forEach((listID,list)->{
					System.out.printf("%1$s%1$s%1$s\"%2$s\": [%3$d]%n", indent, listID, list.size());
					list.forEach(l->{
						System.out.printf("%1$s%1$s%1$s%1$s%2$s%n", indent, l);
					});
				});
			});
			
		}
	
		void addVolatileChild(ListenerSourceParent parent, String listID, ListenerSource child) {
			if (parent==null) throw new IllegalArgumentException();
			if (listID==null) throw new IllegalArgumentException();
			if (child ==null) throw new IllegalArgumentException();
			volatileChildrenOfSources.add(parent, listID, child);
		}
		void addChild(ListenerSourceParent parent, ListenerSource child) {
			if (parent==null) throw new IllegalArgumentException();
			if (child ==null) throw new IllegalArgumentException();
			childrenOfSources.add(parent, child);
		}
		
		void removeListenersOfVolatileChildren(ListenerSourceParent parent) {
			if (parent==null) throw new IllegalArgumentException();
			HashMap<String, Vector<ListenerSource>> childrenLists = volatileChildrenOfSources.remove(parent);
			if (childrenLists != null)
				childrenLists.values().forEach(this::removeListenersOfSources);
		}
		
		void removeListenersOfVolatileChildren(ListenerSourceParent parent, String listID) {
			if (parent==null) throw new IllegalArgumentException();
			if (listID==null) throw new IllegalArgumentException();
			HashMap<String, Vector<ListenerSource>> childrenLists = volatileChildrenOfSources.get(parent);
			removeListenersOfSources(childrenLists==null ? null : childrenLists.remove(listID));
		}
		
		void removeListenersOfSource(ListenerSource source) {
			if (source==null) throw new IllegalArgumentException();
			
			languageListeners            .removeListenersOfSource(source);
			dataReceivers                .removeListenersOfSource(source);
			saveGameListeners            .removeListenersOfSource(source);
			truckToDLCAssignmentListeners.removeListenersOfSource(source);
			
			if (source instanceof ListenerSourceParent) {
				ListenerSourceParent parent = (ListenerSourceParent) source;
				removeListenersOfSources(childrenOfSources.remove(parent));
				removeListenersOfVolatileChildren(parent);
			}
		}

		private void removeListenersOfSources(Vector<ListenerSource> sources) {
			if (sources!=null)
				for (ListenerSource source : sources)
					removeListenersOfSource(source);
		}

		static class AbstractController<Listener> {
			protected final Vector<Listener> listeners = new Vector<>();
			final VectorMap<ListenerSource, Listener> listenersOfSource = new VectorMap<>();
			
			void add(ListenerSource source, Listener l) {
				listenersOfSource.add(source, l);
				listeners.add(l);
			}
			
			void removeListenersOfSource(ListenerSource source) {
				Vector<Listener> list = listenersOfSource.remove(source);
				if (list==null) return;
				listeners.removeAll(list);
			}

			void showListeners(String indent, String label) {
				
				System.out.printf("%s%s.Array: [%d]%n", indent, label, listeners.size());
				for (Listener l : listeners)
					System.out.printf("%s    %s%n", indent, l);
				
				System.out.printf("%s%s.Sources: [%d]%n", indent, label, listenersOfSource.size());
				listenersOfSource.forEach((source,list)->{
					System.out.printf("%s    %s: [%d]%n", indent, source, list.size());
					for (Listener l : list)
						System.out.printf("%s        %s%n", indent, l);
				});
			}
		}
		
		static class LanguageListenerController extends AbstractController<LanguageListener> implements LanguageListener {
			@Override public void setLanguage(Language language) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setLanguage(language);
			}
		}

		static class DataReceiverController extends AbstractController<DataReceiver> implements DataReceiver {
			@Override public void setData(Data data) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setData(data);
			}
		}

		static class SaveGameListenerController extends AbstractController<SaveGameListener> implements SaveGameListener {
			@Override public void setSaveGame(SaveGame saveGame) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setSaveGame(saveGame);
			}
		}

		static class TruckToDLCAssignmentListenerController extends AbstractController<TruckToDLCAssignmentListener> implements TruckToDLCAssignmentListener {
			
			@Override public void setTruckToDLCAssignments(HashMap<String, String> assignments) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setTruckToDLCAssignments(assignments);
			}
			@Override public void updateAfterAssignmentsChange() {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).updateAfterAssignmentsChange();
			}
		}
	}

	static class AppSettings extends Settings<AppSettings.ValueGroup, AppSettings.ValueKey> {
		enum ValueKey {
			WindowX, WindowY, WindowWidth, WindowHeight, SteamLibraryFolder, Language, InitialPAK, SaveGameFolder, SelectedSaveGame, ShowingSaveGameDataSorted,
		}

		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			WindowPos (ValueKey.WindowX, ValueKey.WindowY),
			WindowSize(ValueKey.WindowWidth, ValueKey.WindowHeight),
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		public AppSettings() { super(SnowRunner.class); }
		public Point     getWindowPos (              ) { return getPoint(ValueKey.WindowX,ValueKey.WindowY); }
		public void      setWindowPos (Point location) {        putPoint(ValueKey.WindowX,ValueKey.WindowY,location); }
		public Dimension getWindowSize(              ) { return getDimension(ValueKey.WindowWidth,ValueKey.WindowHeight); }
		public void      setWindowSize(Dimension size) {        putDimension(ValueKey.WindowWidth,ValueKey.WindowHeight,size); }
	}
}
