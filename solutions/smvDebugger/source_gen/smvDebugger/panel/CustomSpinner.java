package smvDebugger.panel;

/*Generated by MPS */

import javax.swing.JSpinner;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.Component;
import javax.swing.SwingConstants;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.border.Border;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.border.CompoundBorder;

public class CustomSpinner extends JSpinner {
  public CustomSpinner(final int size) {
    super();
    setUI(new CustomSpinnerUI(size));
  }

  private static class CustomSpinnerUI extends BasicSpinnerUI {
    private final int size;

    public CustomSpinnerUI(final int size) {
      this.size = size;
    }

    @Override
    protected Component createNextButton() {
      final Component component = createArrowButton(SwingConstants.EAST);
      component.setName("Spinner.nextButton");
      installNextButtonListeners(component);
      return component;
    }

    @Override
    protected Component createPreviousButton() {
      final Component component = createArrowButton(SwingConstants.WEST);
      component.setName("Spinner.previousButton");
      installPreviousButtonListeners(component);
      return component;
    }

    @Override
    protected JComponent createEditor() {
      return new JTextField(size);
    }

    @Override
    public void installUI(final JComponent component) {
      super.installUI(component);
      component.removeAll();
      component.setLayout(new BorderLayout());
      component.add(createNextButton(), BorderLayout.EAST);
      component.add(createPreviousButton(), BorderLayout.WEST);
      component.add(createEditor(), BorderLayout.CENTER);
    }

    private Component createArrowButton(final int direction) {
      final JButton button = new BasicArrowButton(direction);
      final Border border = UIManager.getBorder("Spinner.arrowButtonBorder");
      if (border instanceof UIResource) {
        button.setBorder(new CompoundBorder(border, null));
      } else {
        button.setBorder(border);
      }
      return button;
    }
  }
}