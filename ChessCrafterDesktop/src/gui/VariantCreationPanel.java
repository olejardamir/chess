package gui;

import gui.PieceMakerPanel.PieceListChangedListener;
import gui.PreferenceUtility.PieceToolTipPreferenceChangedListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import logic.Board;
import logic.Builder;
import logic.Game;
import logic.Piece;
import logic.PieceBuilder;
import logic.Square;
import rules.EndOfGame;
import rules.ObjectivePiece;
import rules.ObjectivePiece.ObjectivePieceTypes;
import rules.Rules;
import utility.FileUtility;
import utility.GuiUtility;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dragNdrop.AbstractDropManager;
import dragNdrop.DropAdapter;
import dragNdrop.DropEvent;
import dragNdrop.GlassPane;
import dragNdrop.MotionAdapter;

public class VariantCreationPanel extends ChessPanel implements PieceListChangedListener
{
	public VariantCreationPanel(String variantName)
	{
		mDropManager = new DropManager();
		mGlobalGlassPane = new GlassPane();
		mGlobalGlassPane.setOpaque(false);
		Driver.getInstance().setGlassPane(mGlobalGlassPane);
		m_motionAdapter = new MotionAdapter(mGlobalGlassPane);
		mPieceListPanel = new JPanel();
		mPieceListPanel.setOpaque(false);

		Game gameToEdit = null;
		if (variantName != null)
		{
			gameToEdit = Builder.newGame(variantName);

			mWhiteTeam = gameToEdit.getWhiteTeam();
			mBlackTeam = gameToEdit.getBlackTeam();

			mWhiteRules = gameToEdit.getWhiteRules();
			mBlackRules = gameToEdit.getBlackRules();
			mWhitePromotionMap = gameToEdit.getWhitePromotionMap();
			if (mWhitePromotionMap == null)
				mWhitePromotionMap = Maps.newHashMap();

			mBlackPromotionMap = gameToEdit.getBlackPromotionMap();
			if (mBlackPromotionMap == null)
				mBlackPromotionMap = Maps.newHashMap();

			mBoardPanels = new JPanel[gameToEdit.getBoards().length];

			for (int i = 0; i < mBoardPanels.length; i++)
				mBoardPanels[i] = new JPanel();

			mBuilder = new Builder(variantName, gameToEdit.getBoards(), mWhiteTeam, mBlackTeam, mWhiteRules, mBlackRules);
		}
		else
		{
			mBuilder = new Builder(Messages.getString("VariantCreationPanel.newVariant")); //$NON-NLS-1$

			mWhiteTeam = Lists.newArrayList();
			mBlackTeam = Lists.newArrayList();

			mWhiteRules = new Rules(false);
			mBlackRules = new Rules(true);

			mWhitePromotionMap = Maps.newHashMap();
			mBlackPromotionMap = Maps.newHashMap();
			mBoardPanels = new JPanel[] { new JPanel(), new JPanel() };

			mBuilder.mBlackTeam = mBlackTeam;
			mBuilder.mWhiteTeam = mWhiteTeam;
		}

		mDisplayBoard = new Board(2, 1, false);
		mPieceDisplaySquares[WHITE_INDEX] = mDisplayBoard.getSquare(1, 1);
		mPieceDisplaySquares[BLACK_INDEX] = mDisplayBoard.getSquare(2, 1);

		mOptionsFrame = new JFrame();
		mOptionsFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		mScrollPane.setPreferredSize(new Dimension(200, 200));

		initGUIComponents(gameToEdit, variantName);
	}

	private void initGUIComponents(final Game game, String variantName)
	{
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		setBorder(BorderFactory.createLoweredBevelBorder());

		final JPanel showPiecePanel = new JPanel();
		showPiecePanel.setLayout(new GridLayout(2, 1));
		showPiecePanel.setPreferredSize(new Dimension(50, 100));
		showPiecePanel.setOpaque(false);

		for (Square square : mPieceDisplaySquares)
			showPiecePanel.add(square);

		constraints.gridx = 5;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.insets = new Insets(0, 10, 100, 0);
		add(showPiecePanel, constraints);

		setupPiecesList();

		for (Square square : mPieceDisplaySquares)
		{
			square.addMouseListener(new PieceDisplayBoardListener(square));
			square.addMouseMotionListener(m_motionAdapter);
		}

		mPieceDisplaySquares[WHITE_INDEX].setBackgroundColor(Color.LIGHT_GRAY);
		mPieceDisplaySquares[BLACK_INDEX].setBackgroundColor(Color.getHSBColor(30, 70, 70));

		if (game == null)
		{
			mWhiteRules.addEndOfGame(EndOfGame.CLASSIC.init(0, Messages.getString("VariantCreationPanel.empty"), false)); //$NON-NLS-1$
			mBlackRules.addEndOfGame(EndOfGame.CLASSIC.init(0, Messages.getString("VariantCreationPanel.empty"), true)); //$NON-NLS-1$

			mWhiteRules.setObjectivePiece(new ObjectivePiece(ObjectivePieceTypes.CLASSIC));
			mBlackRules.setObjectivePiece(new ObjectivePiece(ObjectivePieceTypes.CLASSIC));
		}

		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(10, 10, 10, 5);
		constraints.anchor = GridBagConstraints.CENTER;
		add(GuiUtility.createJLabel(Messages.getString("VariantCreationPanel.variantName")), constraints); //$NON-NLS-1$

		final JTextField variantNameField = new JTextField(25);
		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(0, 0, 0, 0);
		add(variantNameField, constraints);

		Board[] temp = new Board[game == null ? 1 : game.getBoards().length];

		if (game == null)
		{
			GuiUtility.requestFocus(variantNameField);
			temp[0] = new Board(8, 8, false);
		}
		else
		{
			variantNameField.setText(variantName);
			for (int i = 0; i < game.getBoards().length; i++)
			{
				temp[i] = game.getBoards()[i];
			}
		}

		drawBoards(temp);

		// main menu button
		JButton returnToMainButton = new JButton(Messages.getString("VariantCreationPanel.returnToMainMenu")); //$NON-NLS-1$
		// returnToMainButton.setToolTipText("Press to return to the Main Menu");
		returnToMainButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				Driver.getInstance().revertToMainPanel();
			}
		});

		// Create button and add ActionListener
		JButton submitButton = new JButton(Messages.getString("VariantCreationPanel.saveAndQuit")); //$NON-NLS-1$
		// submitButton.setToolTipText("Save a finished variant");
		submitButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				if (variantNameField.getText().trim().isEmpty())
				{
					JOptionPane.showMessageDialog(
							VariantCreationPanel.this,
							Messages.getString("VariantCreationPanel.enterAName"), Messages.getString("VariantCreationPanel.enterName"), //$NON-NLS-1$ //$NON-NLS-2$
							JOptionPane.PLAIN_MESSAGE);
					return;
				}

				mWhiteRules.setGame(game);
				mBlackRules.setGame(game);

				mBuilder.setName(variantNameField.getText());

				for (Piece piece : mWhiteTeam)
					piece.setPromotesTo(mWhitePromotionMap.get(piece.getName()));
				for (Piece piece : mBlackTeam)
					piece.setPromotesTo(mBlackPromotionMap.get(piece.getName()));

				int numberOfObjectives = 0;

				if (mWhiteRules.getObjectiveName() != null && !mWhiteRules.getObjectiveName().isEmpty())
				{
					for (Piece piece : mWhiteTeam)
					{
						if (piece.getName().equals(mWhiteRules.getObjectiveName()))
							numberOfObjectives++;
					}
					if (numberOfObjectives != 1)
					{
						JOptionPane.showMessageDialog(Driver.getInstance(),
								Messages.getString("VariantCreationPanel.placeOneWhiteObjective"), //$NON-NLS-1$
								Messages.getString("VariantCreationPanel.objectiveMissing"), JOptionPane.PLAIN_MESSAGE); //$NON-NLS-1$
						return;
					}
				}

				numberOfObjectives = 0;
				if (mBlackRules.getObjectiveName() != null && !mBlackRules.getObjectiveName().isEmpty())
				{
					for (Piece piece : mBlackTeam)
					{
						if (piece.getName().equals(mBlackRules.getObjectiveName()))
							numberOfObjectives++;
					}
					if (numberOfObjectives != 1)
					{
						JOptionPane.showMessageDialog(Driver.getInstance(),
								Messages.getString("VariantCreationPanel.placeOneBlackObjective"), //$NON-NLS-1$
								Messages.getString("VariantCreationPanel.objectiveMissing"), JOptionPane.PLAIN_MESSAGE); //$NON-NLS-1$
						return;
					}
				}

				mBuilder.mWhiteTeam = mWhiteTeam;

				if (mWhiteRules.getObjectiveName() == null)
					mWhiteRules.setObjectivePiece(new ObjectivePiece(ObjectivePieceTypes.CLASSIC));

				mBuilder.mBlackTeam = mBlackTeam;

				if (mBlackRules.getObjectiveName() == null)
					mBlackRules.setObjectivePiece(new ObjectivePiece(ObjectivePieceTypes.CLASSIC));

				mBuilder.writeFile(mWhiteRules, mBlackRules);
				// Return to the main screen.
				Driver.getInstance().revertToMainPanel();
			}
		});

		mChangePromotionButton = new JButton(Messages.getString("VariantCreationPanel.promoteThisPiece")); //$NON-NLS-1$
		mChangePromotionButton.setToolTipText(Messages.getString("VariantCreationPanel.pressToSetUpPromotion")); //$NON-NLS-1$
		mChangePromotionButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				mOptionsFrame.dispose();
				mOptionsFrame = new JFrame(Messages.getString("VariantCreationPanel.piecePromotion")); //$NON-NLS-1$
				new PiecePromotionPanel((String) mPieceTypeList.getSelectedValue(), VariantCreationPanel.this, mOptionsFrame);
			}
		});

		JButton boardSetupButton = new JButton(Messages.getString("VariantCreationPanel.customizeGameBoard")); //$NON-NLS-1$
		boardSetupButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				mOptionsFrame.dispose();
				mOptionsFrame = new JFrame(Messages.getString("VariantCreationPanel.customBoardSetup")); //$NON-NLS-1$

				new CustomBoardPanel(VariantCreationPanel.this, mOptionsFrame);
			}
		});

		JButton objectiveSetupButton = new JButton(Messages.getString("VariantCreationPanel.setUpObjectives")); //$NON-NLS-1$
		objectiveSetupButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				mOptionsFrame.dispose();
				mOptionsFrame = new JFrame();

				new ObjectiveMakerPanel(VariantCreationPanel.this, mOptionsFrame);
			}
		});

		JButton ruleSetupButton = new JButton(Messages.getString("VariantCreationPanel.setUpGameRules")); //$NON-NLS-1$
		ruleSetupButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				mOptionsFrame.dispose();
				mOptionsFrame = new JFrame();

				new RuleMakerPanel(VariantCreationPanel.this, mOptionsFrame);
			}
		});

		JButton playerSetupButton = new JButton(Messages.getString("VariantCreationPanel.setUpPlayerRules")); //$NON-NLS-1$
		playerSetupButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				mOptionsFrame.dispose();
				mOptionsFrame = new JFrame();
				new CustomPlayerPanel(VariantCreationPanel.this, mOptionsFrame);
			}
		});

		JButton pieceSetupButton = new JButton(Messages.getString("VariantCreationPanel.pieceEditor")); //$NON-NLS-1$
		pieceSetupButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				mOptionsFrame.dispose();
				mOptionsFrame = new JFrame();
				(new PieceMenuPanel(mOptionsFrame)).setPieceListChangedListener(VariantCreationPanel.this);
			}
		});

		GridBagConstraints mainPanelConstraints = new GridBagConstraints();

		mPieceListPanel.setLayout(new GridBagLayout());
		mainPanelConstraints.gridx = 0;
		mainPanelConstraints.gridy = 0;
		mainPanelConstraints.gridwidth = 2;
		mPieceListPanel.add(mScrollPane, mainPanelConstraints);

		mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		mainPanelConstraints.gridx = 0;
		mainPanelConstraints.gridy = 1;
		mainPanelConstraints.gridwidth = 1;
		mainPanelConstraints.insets = new Insets(5, 3, 3, 3);
		mPieceListPanel.add(mChangePromotionButton, mainPanelConstraints);

		mainPanelConstraints.gridx = 1;
		mainPanelConstraints.gridy = 1;
		mPieceListPanel.add(pieceSetupButton, mainPanelConstraints);

		mainPanelConstraints.gridx = 0;
		mainPanelConstraints.gridy = 2;
		mPieceListPanel.add(boardSetupButton, mainPanelConstraints);

		mainPanelConstraints.gridx = 0;
		mainPanelConstraints.gridy = 3;
		mPieceListPanel.add(objectiveSetupButton, mainPanelConstraints);

		mainPanelConstraints.gridx = 1;
		mainPanelConstraints.gridy = 3;
		mPieceListPanel.add(ruleSetupButton, mainPanelConstraints);

		mainPanelConstraints.gridx = 1;
		mainPanelConstraints.gridy = 2;
		mPieceListPanel.add(playerSetupButton, mainPanelConstraints);

		JPanel optionsPanel = new JPanel();
		optionsPanel.setOpaque(false);
		optionsPanel.setLayout(new GridBagLayout());

		mainPanelConstraints.fill = GridBagConstraints.CENTER;
		mainPanelConstraints.gridx = 0;
		mainPanelConstraints.gridy = 1;
		optionsPanel.add(submitButton, mainPanelConstraints);

		mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		mainPanelConstraints.gridx = 1;
		mainPanelConstraints.gridy = 1;
		optionsPanel.add(returnToMainButton, mainPanelConstraints);

		constraints.gridy = 2;
		constraints.gridx = 1;
		constraints.gridwidth = 2;
		constraints.insets = new Insets(10, 0, 10, 5);
		add(optionsPanel, constraints);

		mainPanelConstraints.fill = GridBagConstraints.NONE;
		mainPanelConstraints.insets = new Insets(0, 0, 0, 0);
		mainPanelConstraints.gridwidth = 1;
		mainPanelConstraints.gridx = 6;
		mainPanelConstraints.gridy = 1;
		add(mPieceListPanel, mainPanelConstraints);

	}

	public void drawBoards(Board[] boards)
	{
		// Get the set the board into the builder.
		mBuilder.setBoards(boards);
		mGameBoards = boards;

		for (JPanel panel : mBoardPanels)
		{
			panel.removeAll();
			remove(panel);
		}

		mBoardPanels = new JPanel[boards.length];

		GridBagConstraints constraints = new GridBagConstraints();

		List<Square> squareList = Lists.newArrayListWithExpectedSize(boards[0].getMaxRow() * boards[0].getMaxCol() * 2);
		for (int boardIndex = 0, gridxConstraint = 1; boardIndex < boards.length; boardIndex++, gridxConstraint += 2)
		{
			// create a JPanel to hold the grid and set the layout to the number
			// of squares in the board
			mBoardPanels[boardIndex] = new JPanel();
			mBoardPanels[boardIndex].setLayout(new GridLayout(boards[boardIndex].numRows(), boards[boardIndex].numCols()));

			int numberOfRows = boards[boardIndex].numRows();
			int numberOfColumns = boards[boardIndex].numCols();
			for (int row = numberOfRows; row > 0; row--)
			{
				for (int column = 1; column <= numberOfColumns; column++)
				{
					Square square = boards[boardIndex].getSquare(row, column);
					if (square.isOccupied())
					{
						for (int i = 0; i < mPieceTypeList.getModel().getSize(); i++)
						{
							if (!mPieceTypeList.getModel().getElementAt(i).toString().equals(square.getPiece().getName()))
								continue;

							Piece platonicIdeal = null;
							try
							{
								platonicIdeal = PieceBuilder.makePiece(mPieceTypeList.getModel().getElementAt(i).toString(), square
										.getPiece().isBlack(), square, boards[boardIndex]);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
							if (!square.getPiece().equals(platonicIdeal))
								square.setPiece(platonicIdeal);

						}
						square.addMouseListener(new PieceNormalBoardListener(square));
						square.addMouseMotionListener(m_motionAdapter);
					}
					else
					{
						square.addMouseListener(new SquareSetupMouseListener(square));
					}
					squareList.add(square);
					mBoardPanels[boardIndex].add(square);
					square.refresh();
				}
			}

			mBoardPanels[boardIndex].setBorder(BorderFactory.createEtchedBorder(Color.BLACK, Color.GRAY));
			mBoardPanels[boardIndex].setLayout(new GridLayout(boards[boardIndex].numRows(), boards[boardIndex].numCols()));
			// set the size of the grid to the number of rows and columns,
			// scaled by 48, the size of the images
			mBoardPanels[boardIndex].setPreferredSize(new Dimension(boards[boardIndex].numCols() * 48,
					boards[boardIndex].numRows() * 48));

			constraints.insets = new Insets(3, 5, 3, 10);
			constraints.gridx = gridxConstraint;
			constraints.gridy = 1;
			constraints.gridwidth = 2;

			add(mBoardPanels[boardIndex], constraints);
		}
		mDropManager.setComponentList(squareList);

		for (JPanel panel : mBoardPanels)
		{
			panel.revalidate();
			panel.repaint();
		}

		Driver.getInstance().pack();
	}

	public void setupPiecesList()
	{
		// create a List with a vertical ScrollBar
		final DefaultListModel list = new DefaultListModel();

		Object[] standardPieces = PieceBuilder.getSet().toArray();
		for (int i = 0; i < standardPieces.length; i++)
			if (!list.contains(standardPieces[i]))
				list.addElement(standardPieces[i]);

		String[] customPieces = FileUtility.getCustomPieceArray();
		for (int i = 0; i < customPieces.length; i++)
			if (!list.contains(customPieces[i]))
				list.addElement(customPieces[i]);

		mPieceTypeList = new JList(list);

		mPieceTypeList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		mPieceTypeList.setLayoutOrientation(JList.VERTICAL);
		mPieceTypeList.setVisibleRowCount(-1);
		mPieceTypeList.setSelectedIndex(0);

		updateDisplaySquares();

		ListSelectionModel selectList = mPieceTypeList.getSelectionModel();

		mScrollPane.getViewport().add(mPieceTypeList, null);

		selectList.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent event)
			{
				updateDisplaySquares();
			}
		});

		mPieceListPanel.revalidate();
		mPieceListPanel.repaint();

		Driver.getInstance().pack();
	}

	public Builder getBuilder()
	{
		return mBuilder;
	}

	public void setWhiteRules(Rules whiteRules)
	{
		mWhiteRules = whiteRules;
	}

	public void setBlackRules(Rules blackRules)
	{
		mBlackRules = blackRules;
	}

	private final class SquareSetupMouseListener extends MouseAdapter
	{
		public SquareSetupMouseListener(Square square)
		{
			m_square = square;
			if (m_square.isOccupied())
			{
				m_square.setToolTipText(m_square.getPiece().getName());
				m_square.refresh();
			}
		}

		@Override
		public void mouseClicked(MouseEvent event)
		{
			if (m_square.isOccupied())
			{
				Piece toRemove = m_square.setPiece(null);
				(toRemove.isBlack() ? mBlackTeam : mWhiteTeam).remove(toRemove);

				m_square.refresh();
			}
			else
			{
				showSquareOptions();
			}
		}

		private void showSquareOptions()
		{
			final JFrame popupFrame = new JFrame(Messages.getString("VariantCreationPanel.squareOptions")); //$NON-NLS-1$
			popupFrame.setLocationRelativeTo(Driver.getInstance());
			popupFrame.setLayout(new FlowLayout());

			final ChessPanel popupPanel = new ChessPanel();
			popupPanel.setLayout(new GridBagLayout());

			GridBagConstraints constraints = new GridBagConstraints();

			final JButton colorChooserButton = new JButton(Messages.getString("VariantCreationPanel.setSquareColor")); //$NON-NLS-1$
			colorChooserButton.addActionListener(new ActionListener()
			{

				@Override
				public void actionPerformed(ActionEvent event)
				{
					Color color = JColorChooser.showDialog(popupFrame,
							Messages.getString("VariantCreationPanel.chooseColor"), m_square.getColor()); //$NON-NLS-1$
					if (color == null)
						return;
					// TODO: verify that this can be removed
					// can't let them pick exactly the highlight color, or they
					// could move to that space from anywhere
					if (color != Square.HIGHLIGHT_COLOR)
					{
						m_square.setBackgroundColor(color);
						colorChooserButton.setBackground(color);
					}
					else
					{
						// the chances of this happening is EXTREMELY small...
						JOptionPane.showMessageDialog(
								popupFrame,
								Messages.getString("VariantCreationPanel.colorCannotBeSelected"), Messages.getString("VariantCreationPanel.invalidColor"), //$NON-NLS-1$ //$NON-NLS-2$
								JOptionPane.PLAIN_MESSAGE);
					}

				}
			});
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.insets = new Insets(5, 5, 5, 5);
			popupPanel.add(colorChooserButton, constraints);

			final JCheckBox uninhabitableButton = new JCheckBox(
					"<html><font color=#FFFFFF>" + Messages.getString("VariantCreationPanel.uninhabited") + "</font></html>", !m_square.isHabitable()); //$NON-NLS-1$
			uninhabitableButton.setOpaque(false);

			constraints.gridy = 1;
			popupPanel.add(uninhabitableButton, constraints);

			final JButton doneButton = new JButton(Messages.getString("VariantCreationPanel.done")); //$NON-NLS-1$
			doneButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent event)
				{
					if (uninhabitableButton.isSelected())
						m_square.setIsHabitable(false);
					else
						m_square.setIsHabitable(true);
					popupFrame.dispose();
				}
			});
			constraints.gridy = 2;
			constraints.insets = new Insets(10, 5, 10, 5);
			popupPanel.add(doneButton, constraints);

			popupFrame.add(popupPanel);

			popupFrame.pack();
			popupFrame.setVisible(true);
		}

		private Square m_square;
	}

	public void putPromotionMap(String pieceName, List<String> promotesTo, int colorCode)
	{
		if (colorCode == Builder.BLACK || colorCode == Builder.BOTH)
		{
			mBlackPromotionMap.put(pieceName, promotesTo);
			if (mBuilder.getBlackPromotionMap() == null)
				mBuilder.setBlackPromotionMap(mBlackPromotionMap);
			mBuilder.addToPromotionMap(pieceName, promotesTo, colorCode);
		}
		if (colorCode == Builder.WHITE || colorCode == Builder.BOTH)
		{
			mWhitePromotionMap.put(pieceName, promotesTo);
			if (mBuilder.getWhitePromotionMap() == null)
				mBuilder.setWhitePromotionMap(mWhitePromotionMap);
			mBuilder.addToPromotionMap(pieceName, promotesTo, colorCode);
		}
	}

	public Map<String, List<String>> getWhitePromotionMap()
	{
		return mWhitePromotionMap;
	}

	public Map<String, List<String>> getBlackPromotionMap()
	{
		return mBlackPromotionMap;
	}

	private final class PieceDisplayBoardListener extends DropAdapter implements MouseListener, PieceToolTipPreferenceChangedListener
	{
		public PieceDisplayBoardListener(Square square)
		{
			super(mGlobalGlassPane);
			m_square = square;
			addDropListener(mDropManager);
			if (m_square.getPiece() != null && PreferenceUtility.getPreference().showPieceToolTips())
				m_square.setToolTipText(m_square.getPiece().getToolTipText());
			PreferenceUtility.addPieceToolTipListener(this);
		}

		public void onPieceSelectionChanged()
		{
			if (m_square.getPiece() != null && PreferenceUtility.getPreference().showPieceToolTips())
				m_square.setToolTipText(m_square.getPiece().getToolTipText());
		}

		@Override
		public void mousePressed(MouseEvent event)
		{
			mGlassPane.setVisible(true);

			Point point = (Point) event.getPoint().clone();
			SwingUtilities.convertPointToScreen(point, event.getComponent());
			SwingUtilities.convertPointFromScreen(point, mGlassPane);

			mGlassPane.setPoint(point);

			BufferedImage image = null;
			ImageIcon imageIcon = m_square.getPiece().getIcon();
			int width = imageIcon.getIconWidth();
			int height = imageIcon.getIconHeight();
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics2D = (Graphics2D) image.getGraphics();
			imageIcon.paintIcon(null, graphics2D, 0, 0);
			graphics2D.dispose();

			mGlassPane.setImage(image);
			mGlassPane.repaint();
		}

		@Override
		public void mouseReleased(MouseEvent event)
		{
			Point point = (Point) event.getPoint().clone();
			SwingUtilities.convertPointToScreen(point, event.getComponent());

			mGlassPane.setImage(null);
			mGlassPane.setVisible(false);

			fireDropEvent(new DropEvent(point, m_square), true);
		}

		@Override
		public void mouseExited(MouseEvent event)
		{
		}

		@Override
		public void mouseEntered(MouseEvent event)
		{
		}

		@Override
		public void mouseClicked(MouseEvent event)
		{
		}

		private final Square m_square;

		@Override
		public void onPieceToolTipPreferenceChanged()
		{
			if (m_square.getPiece() != null && PreferenceUtility.getPreference().showPieceToolTips())
				m_square.setToolTipText(m_square.getPiece().getToolTipText());
			else
				m_square.setToolTipText(null);
		}
	};

	private final class PieceNormalBoardListener extends DropAdapter implements MouseListener, PieceToolTipPreferenceChangedListener
	{
		public PieceNormalBoardListener(Square square)
		{
			super(mGlobalGlassPane);
			m_square = square;
			addDropListener(mDropManager);
			if (m_square.getPiece() != null && PreferenceUtility.getPreference().showPieceToolTips())
				m_square.setToolTipText(m_square.getPiece().getToolTipText());
			PreferenceUtility.addPieceToolTipListener(this);
		}

		@Override
		public void mousePressed(MouseEvent event)
		{
			mGlassPane.setVisible(true);

			Point point = (Point) event.getPoint().clone();
			SwingUtilities.convertPointToScreen(point, event.getComponent());
			SwingUtilities.convertPointFromScreen(point, mGlassPane);

			mGlassPane.setPoint(point);

			BufferedImage image = null;
			String pieceName = m_square.getPiece().getName();

			FileUtility.getPieceFile(pieceName);

			ImageIcon imageIcon = m_square.getPiece().getIcon();
			int width = imageIcon.getIconWidth();
			int height = imageIcon.getIconHeight();
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics2D = (Graphics2D) image.getGraphics();
			imageIcon.paintIcon(null, graphics2D, 0, 0);
			graphics2D.dispose();
			mGlassPane.setImage(image);
			m_square.setIcon(null);
			mGlassPane.repaint();
		}

		@Override
		public void mouseReleased(MouseEvent event)
		{
			Point point = (Point) event.getPoint().clone();
			SwingUtilities.convertPointToScreen(point, event.getComponent());

			mGlassPane.setImage(null);
			mGlassPane.setVisible(false);

			fireDropEvent(new DropEvent(point, m_square), false);
		}

		public void onPieceSelectionChanged()
		{
			if (m_square.getPiece() != null)
				m_square.setToolTipText(m_square.getPiece().getToolTipText());
		}

		@Override
		public void mouseExited(MouseEvent event)
		{
		}

		@Override
		public void mouseEntered(MouseEvent event)
		{
		}

		@Override
		public void mouseClicked(MouseEvent event)
		{
		}

		@Override
		public void onPieceToolTipPreferenceChanged()
		{
			if (m_square.getPiece() != null && PreferenceUtility.getPreference().showPieceToolTips())
				m_square.setToolTipText(m_square.getPiece().getToolTipText());
			else
				m_square.setToolTipText(null);
		}

		private final Square m_square;
	};

	private final class DropManager extends AbstractDropManager
	{
		@Override
		public void dropped(DropEvent event, boolean fromDisplayBoard)
		{
			Square originSquare = (Square) event.getOriginComponent();
			Square destinationSquare = (Square) isInTarget(event.getDropLocation());

			Piece originPiece = originSquare.getPiece();

			if (originPiece == null)
				return;

			if (!fromDisplayBoard)
			{
				(originPiece.isBlack() ? mBlackTeam : mWhiteTeam).remove(originPiece);

				// drag piece off board to remove it
				if (destinationSquare == null)
				{
					originSquare.setPiece(null);

					for (MouseListener listener : originSquare.getMouseListeners())
						originSquare.removeMouseListener(listener);

					originSquare.addMouseListener(new SquareSetupMouseListener(originSquare));

					originSquare.refresh();
					return;
				}
			}

			if (destinationSquare != null && destinationSquare.isHabitable())
			{
				Piece oldPiece = destinationSquare.getPiece();
				if (oldPiece != null)
					(oldPiece.isBlack() ? mBlackTeam : mWhiteTeam).remove(oldPiece);

				int boardNumber = event.getDropLocation().getX() < mBoardPanels[0].getLocationOnScreen().getX()
						+ mBoardPanels[0].getWidth() ? 0 : 1;

				Piece piece = null;
				try
				{
					piece = PieceBuilder.makePiece(originPiece.getName(), originPiece.isBlack(), destinationSquare,
							mBuilder.getBoards()[boardNumber]);
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(Driver.getInstance(),
							Messages.getString("VariantCreationPanel.errorCouldNotLoadPiece")); //$NON-NLS-1$
					e.printStackTrace();
					return;
				}

				if (originPiece.isBlack())
					mBlackTeam.add(piece);
				else
					mWhiteTeam.add(piece);

				if (!fromDisplayBoard)
				{
					originSquare.setPiece(null);

					originSquare.refresh();

					// why is there no "clear listeners"?
					for (MouseListener mouseListener : originSquare.getMouseListeners())
					{
						originSquare.removeMouseListener(mouseListener);
					}
					originSquare.addMouseListener(new SquareSetupMouseListener(originSquare));
				}
				for (MouseListener mouseListener : destinationSquare.getMouseListeners())
				{
					destinationSquare.removeMouseListener(mouseListener);
				}
				destinationSquare.addMouseListener(new PieceNormalBoardListener(destinationSquare));
				destinationSquare.addMouseMotionListener(m_motionAdapter);

				destinationSquare.setPiece(piece);
				destinationSquare.refresh();

			}
			else
			{
				JOptionPane
						.showMessageDialog(
								Driver.getInstance(),
								Messages.getString("VariantCreationPanel.squareIsUninhabitable"), Messages.getString("VariantCreationPanel.warning"), //$NON-NLS-1$ //$NON-NLS-2$
								JOptionPane.PLAIN_MESSAGE);
			}
		}
	};

	@Override
	public void onPieceListChanged()
	{
		setupPiecesList();
		drawBoards(mGameBoards);
		updateDisplaySquares();
	}

	private void updateDisplaySquares()
	{
		Piece whitePieceBeingDisplayed = null;
		Piece blackPieceBeingDisplayed = null;

		try
		{
			whitePieceBeingDisplayed = PieceBuilder.makePiece(mPieceTypeList.getSelectedValue().toString(), false,
					mPieceDisplaySquares[WHITE_INDEX], mDisplayBoard);
			blackPieceBeingDisplayed = PieceBuilder.makePiece(mPieceTypeList.getSelectedValue().toString(), true,
					mPieceDisplaySquares[BLACK_INDEX], mDisplayBoard);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(Driver.getInstance(), Messages.getString("VariantCreationPanel.errorCouldNotLoadPiece")); //$NON-NLS-1$
			e.printStackTrace();
			return;
		}

		mPieceDisplaySquares[WHITE_INDEX].setPiece(whitePieceBeingDisplayed);
		mPieceDisplaySquares[BLACK_INDEX].setPiece(blackPieceBeingDisplayed);

		for (Square square : mPieceDisplaySquares)
		{
			square.resetColor();
			square.refresh();
			for (MouseListener listener : square.getMouseListeners())
			{
				if (listener instanceof PieceDisplayBoardListener)
					((PieceDisplayBoardListener) listener).onPieceSelectionChanged();
				else if (listener instanceof PieceNormalBoardListener)
					((PieceNormalBoardListener) listener).onPieceSelectionChanged();
			}
		}
	}

	private static final long serialVersionUID = 7830479492072657640L;
	private static final int WHITE_INDEX = 0;
	private static final int BLACK_INDEX = 1;

	private final GlassPane mGlobalGlassPane;
	private final DropManager mDropManager;
	private final Square[] mPieceDisplaySquares = new Square[2];

	public Rules mWhiteRules;
	public Rules mBlackRules;

	private JPanel[] mBoardPanels;
	private JPanel mPieceListPanel;
	private Map<String, List<String>> mWhitePromotionMap;
	private Map<String, List<String>> mBlackPromotionMap;
	private Builder mBuilder;
	private List<Piece> mWhiteTeam;
	private List<Piece> mBlackTeam;
	private JButton mChangePromotionButton;
	private JList mPieceTypeList;
	private JFrame mOptionsFrame;
	private JScrollPane mScrollPane = new JScrollPane();
	private MotionAdapter m_motionAdapter;
	private Board mDisplayBoard;
	private Board[] mGameBoards;
}
