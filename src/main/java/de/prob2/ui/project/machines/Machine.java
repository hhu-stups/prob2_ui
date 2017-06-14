package de.prob2.ui.project.machines;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;

import de.prob2.ui.verifications.ltl.LTLCheckableItem;
import de.prob2.ui.verifications.ltl.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class Machine extends LTLCheckableItem {
	@FunctionalInterface
	public interface Loader extends Serializable {
		StateSpace load(final Api api, final String file, final Map<String, String> prefs) throws IOException, ModelTranslationError;
	}

	public enum Type {
		B(Api::b_load, new String[] {"*.mch", "*.ref", "*.imp"}),
		EVENTB(Api::eventb_load, new String[] {"*.eventb", "*.bum", "*.buc"}),
		CSP((api, file, prefs) -> { // FIXME Replace this lambda with Api::csp_load once the kernel builds again
			try {
				return api.csp_load(file, prefs);
			} catch (IOException | ModelTranslationError | RuntimeException | Error e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}, new String[] {"*.cspm"}),
		TLA(Api::tla_load, new String[] {"*.tla"}),
		;
		
		private final Machine.Loader loader;
		private final String[] extensions;
		
		private Type(final Machine.Loader loader, final String[] extensions) {
			this.loader = loader;
			this.extensions = extensions;
		}
		
		public Machine.Loader getLoader() {
			return this.loader;
		}
		
		public String[] getExtensions() {
			return this.extensions.clone();
		}
	}
	
	public static final class FileAndType {
		private final File file;
		private final Machine.Type type;
		
		public FileAndType(final File file, final Machine.Type type) {
			super();
			
			Objects.requireNonNull(file);
			Objects.requireNonNull(type);
			
			this.file = file;
			this.type = type;
		}
		
		public File getFile() {
			return this.file;
		}
		
		public Machine.Type getType() {
			return this.type;
		}
	}
	
	private String location;
	private Machine.Type type;
	private ListProperty<LTLFormulaItem> ltlFormulas;
	private ListProperty<LTLPatternItem> ltlPatterns;

	public Machine(String name, String description, Path location, Machine.Type type) {
		super(name,description);
		this.location = location.toString();
		this.type = type;
		this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
	}
	
	public static Machine.FileAndType askForFile(final Window window) {
		final FileChooser.ExtensionFilter classicalB = new FileChooser.ExtensionFilter("Classical B Files", Machine.Type.B.getExtensions());
		final FileChooser.ExtensionFilter eventB = new FileChooser.ExtensionFilter("EventB Files", Machine.Type.EVENTB.getExtensions());
		final FileChooser.ExtensionFilter csp = new FileChooser.ExtensionFilter("CSP Files", Machine.Type.CSP.getExtensions());
		final FileChooser.ExtensionFilter tla = new FileChooser.ExtensionFilter("TLA Files", Machine.Type.TLA.getExtensions());
		
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select Machine");
		fileChooser.getExtensionFilters().addAll(classicalB, eventB, csp, tla);
		
		final File file = fileChooser.showOpenDialog(window);
		if (file == null) {
			return null;
		}
		
		final FileChooser.ExtensionFilter xf = fileChooser.getSelectedExtensionFilter();
		final Machine.Type type;
		if (xf == classicalB) {
			type = Machine.Type.B;
		} else if (xf == eventB) {
			type = Machine.Type.EVENTB;
		} else if (xf == csp) {
			type = Machine.Type.CSP;
		} else if (xf == tla) {
			type = Machine.Type.TLA;
		} else {
			throw new IllegalArgumentException("Unhandled file type: " + xf);
		}
		
		return new Machine.FileAndType(file, type);
	}

	public String getFileName() {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = location.split(pattern);
		return splittedFileName[splittedFileName.length - 1];
	}
	
	public Machine.Type getType() {
		return this.type;
	}
	
	@Override
	public void initializeStatus() {
		super.initializeStatus();
		if (ltlFormulas != null) {
			for (LTLFormulaItem item : ltlFormulas) {
				item.initializeStatus();
			}
		}
		if (ltlPatterns != null) {
			for (LTLPatternItem item : ltlPatterns) {
				item.initializeStatus();
			}
		}
	}
		
	public ListProperty<LTLFormulaItem> ltlFormulasProperty() {
		return ltlFormulas;
	}
	
	public List<LTLFormulaItem> getFormulas() {
		return ltlFormulasProperty().get();
	}
	
	public void addLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.add(formula);
	}
	
	public void removeLTLFormula(LTLFormulaItem formula) {
		ltlFormulas.remove(formula);
	}
	
	public ListProperty<LTLPatternItem> ltlPatternsProperty() {
		return ltlPatterns;
	}
	
	public List<LTLPatternItem> getPatterns() {
		return ltlPatternsProperty().get();
	}
	
	public void addLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.add(pattern);
	}
	
	public void removeLTLPattern(LTLPatternItem pattern) {
		ltlPatterns.remove(pattern);
	}
	
		
	public void replaceMissingWithDefaults() {
		if (type == null) {
			this.type = Machine.Type.B;
		}
		if(ltlFormulas == null) {
			this.ltlFormulas = new SimpleListProperty<>(this, "ltlFormulas", FXCollections.observableArrayList());
		}
		if(ltlPatterns == null) {
			this.ltlPatterns = new SimpleListProperty<>(this, "ltlPatterns", FXCollections.observableArrayList());
		}
	}
	
	public Path getPath() {
		return Paths.get(location);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Machine)) {
			return false;
		}
		Machine otherMachine = (Machine) other;
		return otherMachine.location.equals(this.location);
	}
	
	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
	

}
