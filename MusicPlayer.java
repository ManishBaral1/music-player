package com.trolltech.examples.phonon;

import com.trolltech.qt.*;
import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;
import com.trolltech.qt.phonon.*;
import com.trolltech.examples.*;

import java.util.*;

@QtJambiExample(name = "Music Player")
public class MusicPlayer extends QMainWindow
{

    public MusicPlayer()
    {
        audioOutput = new AudioOutput(Phonon.Category.MusicCategory);
        mediaObject = new MediaObject(this);
        metaInformationResolver = new MediaObject(this);

        Phonon.createPath(mediaObject, audioOutput);

        mediaObject.setTickInterval(1000);

        mediaObject.tick.connect(this, "tick(long)");
        mediaObject.stateChanged.connect(this, "stateChanged(Phonon$State,Phonon$State)");
        metaInformationResolver.stateChanged.
                connect(this, "metaStateChanged(Phonon$State,Phonon$State)");
        mediaObject.currentSourceChanged.connect(this, "sourceChanged(MediaSource)");
        mediaObject.aboutToFinish.connect(this, "aboutToFinish()");

        setupActions();
        setupMenus();
        setupUi();
        timeLcd.display("00:00"); 
    }

    private void addFiles()
    {
        List<String> files = QFileDialog.getOpenFileNames(this,
                            tr("Select Music Files"), ".");

        if (files.isEmpty())
            return;

        int index = sources.size();
        for (String string : files) {
            MediaSource source = new MediaSource(string);
        
            sources.add(source);
        } 
        if (!sources.isEmpty())
            metaInformationResolver.setCurrentSource(sources.get(index));
    }

    private void about()
    {
        QMessageBox.information(this, tr("About Music Player"),
            tr("The Music Player example shows how to use Phonon - the multimedia" +
               " framework that comes with Qt - to create a simple music player."));
    }

    private void stateChanged(Phonon.State newState, Phonon.State oldState)
    {
        switch (newState) {
            case ErrorState:
                if (mediaObject.errorType().equals(Phonon.ErrorType.FatalError)) {
                    QMessageBox.warning(this, tr("Fatal Error"),
                    mediaObject.errorString());
                } else {
                    QMessageBox.warning(this, tr("Error"),
                    mediaObject.errorString());
                }
                break;
            case PlayingState:
                playAction.setEnabled(false);
                pauseAction.setEnabled(true);
                stopAction.setEnabled(true);
                break;
            case StoppedState:
                stopAction.setEnabled(false);
                playAction.setEnabled(true);
                pauseAction.setEnabled(false);
                timeLcd.display("00:00");
                break;
            case PausedState:
                pauseAction.setEnabled(false);
                stopAction.setEnabled(true);
                playAction.setEnabled(true);
                break;
            case BufferingState:
                break;
        }
    }

    private void tick(long time)
    {
        QTime displayTime = new QTime(0, (int) (time / 60000) % 60, (int) (time / 1000) % 60);

        timeLcd.display(displayTime.toString("mm:ss"));
    }

    private void tableClicked(int row, int column)
    {
        boolean wasPlaying = mediaObject.state().equals(Phonon.State.PlayingState);

        mediaObject.stop();
        mediaObject.clearQueue();

        mediaObject.setCurrentSource(sources.get(row));

        if (wasPlaying) 
            mediaObject.play();
        else
            mediaObject.stop();
    }

    private void sourceChanged(MediaSource source)
    {
        musicTable.selectRow(sources.indexOf(source));

        timeLcd.display("00:00");
    }

    private void metaStateChanged(Phonon.State newState, Phonon.State oldState)
    {
        if (newState.equals(Phonon.State.ErrorState)) {
            QMessageBox.warning(this, tr("Error opening files"),
                metaInformationResolver.errorString());
            while (!sources.isEmpty() &&
                   !(sources.remove(sources.size() - 1).equals(metaInformationResolver.currentSource())));
            return;
        }

        if (!newState.equals(Phonon.State.StoppedState))
            return;

        if (metaInformationResolver.currentSource().type().equals(MediaSource.Type.Invalid))
                return;

        Map<String, List<String>> metaData = metaInformationResolver.metaData();


        String title = "";
        if (metaData.get("TITLE") != null)
            title = metaData.get("TITLE").get(0);

        if (title.equals(""))
            title = metaInformationResolver.currentSource().fileName();

        String artist = "";
        if (metaData.get("ARTIST") != null)
            artist = metaData.get("ARTIST").get(0);

        String album = "";
        if (metaData.get("ALBUM") != null)
            album = metaData.get("ALBUM").get(0);

        String year = "";
        if (metaData.get("DATE") != null)
            year = metaData.get("DATE").get(0);

        QTableWidgetItem titleItem = new QTableWidgetItem(title);
        QTableWidgetItem artistItem = new QTableWidgetItem(artist);
        QTableWidgetItem albumItem = new QTableWidgetItem(album);
        QTableWidgetItem yearItem = new QTableWidgetItem(year);

        int currentRow = musicTable.rowCount();
        musicTable.insertRow(currentRow);
        musicTable.setItem(currentRow, 0, titleItem);
        musicTable.setItem(currentRow, 1, artistItem);
        musicTable.setItem(currentRow, 2, albumItem);
        musicTable.setItem(currentRow, 3, yearItem);

        if (musicTable.selectedItems().isEmpty()) {
            musicTable.selectRow(0);
            mediaObject.setCurrentSource(metaInformationResolver.currentSource());
        }

        MediaSource source = metaInformationResolver.currentSource();
        int index = sources.indexOf(metaInformationResolver.currentSource()) + 1;
        if (sources.size() > index) {
            metaInformationResolver.setCurrentSource(sources.get(index));
        }
        else {
            musicTable.resizeColumnsToContents();
            if (musicTable.columnWidth(0) > 300)
                musicTable.setColumnWidth(0, 300);
        }
    }

    private void aboutToFinish()
    {
        int index = sources.indexOf(mediaObject.currentSource()) + 1;
        if (sources.size() > index) {
            mediaObject.enqueue(sources.get(index));
            musicTable.selectRow(index);
        }
    }

    private void setupActions()
    {
        playAction = new QAction(new QIcon("classpath:com/trolltech/examples/images/play.png"), tr("Play"), this);
        playAction.setShortcut(tr("Crl+P"));
        playAction.setDisabled(true);
        pauseAction = new QAction(new QIcon("classpath:com/trolltech/examples/images/pause.png"), tr("Pause"), this);
        pauseAction.setShortcut(tr("Ctrl+A"));
        pauseAction.setDisabled(true);
        stopAction = new QAction(new QIcon("classpath:com/trolltech/examples/images/stop.png"), tr("Stop"), this);
        stopAction.setShortcut(tr("Ctrl+S"));
        stopAction.setDisabled(true);
        addFilesAction = new QAction(tr("Add &Files"), this);
        addFilesAction.setShortcut(tr("Ctrl+F"));
        exitAction = new QAction(tr("E&xit"), this);
        exitAction.setShortcut(tr("Ctrl+X"));
        aboutAction = new QAction(tr("A&bout"), this);
        aboutAction.setShortcut(tr("Ctrl+B"));
        aboutQtJambiAction = new QAction(tr("About &Qt Jambi"), this);
        aboutQtJambiAction.setShortcut(tr("Ctrl+Q"));
        aboutQtAction = new QAction(tr("About Q&t"), this);

        playAction.triggered.connect(mediaObject, "play()");
        pauseAction.triggered.connect(mediaObject, "pause()");
        stopAction.triggered.connect(mediaObject, "stop()");
        addFilesAction.triggered.connect(this, "addFiles()");
        exitAction.triggered.connect(this, "close()");
        aboutAction.triggered.connect(this, "about()");
        aboutQtAction.triggered.connect(QApplication.instance(), "aboutQt()");
        aboutQtJambiAction.triggered.connect(QApplication.instance(), "aboutQtJambi()");
    }

    private void setupMenus()
    {
        QMenu fileMenu = menuBar().addMenu(tr("&File"));
        fileMenu.addAction(addFilesAction);
        fileMenu.addSeparator();
        fileMenu.addAction(exitAction);

        QMenu aboutMenu = menuBar().addMenu(tr("&Help"));
        aboutMenu.addAction(aboutAction);
        aboutMenu.addAction(aboutQtJambiAction);
        aboutMenu.addAction(aboutQtAction);        
    }

    private void setupUi()
    {
        QToolBar bar = new QToolBar();

        bar.addAction(playAction);
        bar.addAction(pauseAction);
        bar.addAction(stopAction);
    
        seekSlider = new SeekSlider(this);
        seekSlider.setMediaObject(mediaObject);

        volumeSlider = new VolumeSlider(this);
        volumeSlider.setAudioOutput(audioOutput);
        volumeSlider.setSizePolicy(QSizePolicy.Policy.Maximum, QSizePolicy.Policy.Maximum);

        QLabel volumeLabel = new QLabel();
        volumeLabel.setPixmap(new QPixmap("images/volume.png"));

        QPalette palette = new QPalette();
        palette.setBrush(QPalette.ColorRole.Light, new QBrush(QColor.darkGray));

        timeLcd = new QLCDNumber();
        timeLcd.setPalette(palette);

        List<String> headers = new Vector<String>();
        headers.add(tr("Title"));
        headers.add(tr("Artist"));
        headers.add(tr("Album"));
        headers.add(tr("Year"));

        musicTable = new QTableWidget(0, 4);
        musicTable.setHorizontalHeaderLabels(headers);
        musicTable.setSelectionMode(QAbstractItemView.SelectionMode.SingleSelection);
        musicTable.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows);
        musicTable.cellPressed.connect(this, "tableClicked(int,int)");

        QHBoxLayout seekerLayout = new QHBoxLayout();
        seekerLayout.addWidget(seekSlider);
        seekerLayout.addWidget(timeLcd);

        QHBoxLayout playbackLayout = new QHBoxLayout();
        playbackLayout.addWidget(bar);
        playbackLayout.addStretch();
        playbackLayout.addWidget(volumeLabel);
        playbackLayout.addWidget(volumeSlider);

        QVBoxLayout mainLayout = new QVBoxLayout();
        mainLayout.addWidget(musicTable);
        mainLayout.addLayout(seekerLayout);
        mainLayout.addLayout(playbackLayout);

        QWidget widget = new QWidget();
        widget.setLayout(mainLayout);

        setCentralWidget(widget);
        setWindowTitle("Phonon Music Player");
        setWindowIcon(new QIcon("classpath:com/trolltech/images/qt-logo.png"));
    }

    private SeekSlider seekSlider;
    private MediaObject mediaObject;
    private MediaObject metaInformationResolver;
    private AudioOutput audioOutput;
    private VolumeSlider volumeSlider;
    private List<MediaSource> sources =
        new Vector<MediaSource>();

    private QAction playAction;
    private QAction pauseAction;
    private QAction stopAction;
    private QAction addFilesAction;
    private QAction exitAction;
    private QAction aboutAction;
    private QAction aboutQtAction;
    private QAction aboutQtJambiAction;
    private QLCDNumber timeLcd;
    private QTableWidget musicTable;

    public static void main(String args[])
    {
            QApplication.initialize(args);
            QApplication.setApplicationName("Music Player");

            new MusicPlayer().show();

            QApplication.exec();
    }
}