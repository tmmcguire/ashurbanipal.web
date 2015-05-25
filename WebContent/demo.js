(function() {
    "use strict";

    Ext.namespace('PG');

    function coalesce_conversion(name) { return function(v,rec) { return rec[name].join('; '); } }
    function pick_first_conversion(name) { return function(v,rec) { return rec[name][0]; } }

    PG.text_store = new Ext.data.Store({
        proxy: new Ext.data.HttpProxy({
            url: 'data/lookup',
            method: 'GET',
        }),
        reader: new Ext.data.JsonReader({
            root: 'rows',
            totalProperty: 'count',
            id: 'etext_no'
        }, [
            { name: 'author' },
            { name: 'copyright_status' },
            { name: 'etext_no' },
            { name: 'language' },
            { name: 'link' },
            { name: 'loc_class' },
            { name: 'notes' },
            { name: 'release_date' },
            { name: 'subject' },
            { name: 'title' },
        ]),
    });

    PG.bookTpl = new Ext.XTemplate(
        '<div class="book-details"><a href="{link}" style="text-decoration:none; color:#000000;" target="_blank">',
        '<p><b><i>{title},</i></b></p>',
        '<p><b>{author}</b></p>',
        '<p>{subject}</p>',
        '<tpl if="release_date"><p>Released {release_date}</p></tpl>',
        '<tpl if="loc_class"><p>Library of Congress class {loc_class}</p></tpl>',
        '<p>{language}</p>',
        '<p><em>{note}</em></p>',
        '<p>{copyright_status}</p>',
        '<p>Etext #{etext_no}</p>',
        '<tpl if="distance"><hr /><p>{distance}</p></tpl>',
        '</a></div>'
    );

    PG.text_tmpl = new Ext.XTemplate(
        '<tpl for="."><div class="search-item">',
        '<b><i>{title}</i></b>',
        '<tpl if="author"> by <b>{author}</b> </tpl>',
        '<span style="float:right;">Etext #{etext_no}</span><br />',
        '<p style="padding-top:2px; line-height:1.2;">{subject}</p>',
        '</div></tpl>'
    );

    PG.style = {
        url: 'data/style',
        transactionId: undefined,
        rows: undefined,
        current: 1,

        loadMask: new Ext.LoadMask(Ext.get('style-row'), {
            msg: '<h3>Loading...</h3>'
        }),

        success: function(response, options) {
            PG.style.transactionId = undefined;
            PG.style.rows = Ext.decode(response.responseText).rows;
            if (PG.style.rows && PG.style.rows.length) {
                for (var i = 1; i < 4; ++i) {
                    var data = PG.style.rows[i];
                    data.distance = data.dist.toFixed(3) + " ell";
                    PG.bookTpl.overwrite(Ext.get('style' + i), data);
                }
            }
            PG.style.loadMask.hide();
        },

        failure: function(response, options) {
            PG.style.transactionId = undefined;
        }
    };

    PG.topic = {
        url: 'data/topic',
        transactionId: undefined,
        rows: undefined,
        current: 1,

        loadMask: new Ext.LoadMask(Ext.get('topic-row'), {
            msg: '<h3>Loading...</h3>'
        }),

        success: function(response, options) {
            PG.topic.transactionId = undefined;
            PG.topic.rows = Ext.decode(response.responseText).rows;
            if (PG.topic.rows && PG.topic.rows.length) {
                for (var i = 1; i < 4; ++i) {
                    var data = PG.topic.rows[i];
                    data.distance = data.score.toFixed(3) + " bole";
                    PG.bookTpl.overwrite(Ext.get('topic' + i), data);
                }
            }
            PG.topic.loadMask.hide();
        },

        failure: function(response, options) {
            PG.topic.transactionId = undefined;
        }

    };

    PG.combination = {
        url: 'data/combination',
        transactionId: undefined,
        rows: undefined,
        current: 1,

        loadMask: new Ext.LoadMask(Ext.get('combined-row'), {
            msg: '<h3>Loading...</h3>'
        }),

        success: function(response, options) {
            PG.combination.transactionId = undefined;
            PG.combination.rows = Ext.decode(response.responseText).rows;
            if (PG.combination.rows && PG.combination.rows.length) {
                for (var i = 1; i < 4; ++i) {
                    var data = PG.combination.rows[i];
                    data.distance = data.dist_score.toFixed(3) + " ell bole";
                    PG.bookTpl.overwrite(Ext.get('combined' + i), data);
                }
            }
            PG.combination.loadMask.hide();
        },

        failure: function(response, options) {
            PG.combination.transactionId = undefined;
        }

    };

    function cbSelectionChange(combo, record, index) {
        if (!record) { return; }
        selectBook(record.data);
    }

    function selectBook(selectedBook) {
        Ext.History.add(Ext.util.JSON.encode(selectedBook));

        // Remove the distance from the selected data (if set by previous selection)
        selectedBook.distance = "";
        PG.bookTpl.overwrite(Ext.get('book-info'), selectedBook);

        startRequest(PG.style, selectedBook.etext_no);
        startRequest(PG.topic, selectedBook.etext_no);
        startRequest(PG.combination, selectedBook.etext_no);
    }

    function startRequest(query, etext_no) {
        query.loadMask.show();
        query.transactionId = Ext.Ajax.request({
            url: query.url,
            method: 'GET',
            success: query.success,
            failure: query.failure,
            params: { etext_no: etext_no, start: 0, limit: 20 }
        });
    }

    PG.text_searchbox = new Ext.form.ComboBox({
        store: PG.text_store,
        displayField: 'title',
        loadingText: 'Searching...',
        width: 570,
        pageSize: 20,
        hideTrigger: true,
        tpl: PG.text_tmpl,
        applyTo: 'search',
        itemSelector: 'div.search-item',
        listeners: {
            'select': cbSelectionChange,
        },
    });

    // function createGrid() {
    //     PG.grid = new Ext.grid.GridPanel({
    //         store: PG.metadataStore,
    //         renderTo: 'grid-example',
    //         columns: [
    //             { id: 'etext_no', header: 'EText',  dataIndex: 'etext_no', sortable: true, width: 50  },
    //             { id: 'title',    header: 'Title',  dataIndex: 'title',    sortable: true, width: 275 },
    //             { id: 'author',   header: 'Author', dataIndex: 'author',   sortable: true, width: 250 },
    //         ],
    //         stripeRows: true,
    //         height: 350,
    //         width: 600,
    //         selModel: new Ext.grid.RowSelectionModel({
    //             singleSelect: true,
    //             listeners: { 'selectionchange': cbSelectionChange, },
    //         }),
    //         listeners: {
    //             'viewready': function(grid) {
    //                 var row = PG.metadataStore.indexOf( PG.metadataStore.getById(773) );
    //                 grid.getSelectionModel().selectRow(row,false,false);
    //                 grid.getView().focusRow(row);
    //                 PG.loadMask.hide();
    //             }
    //         }
    //     });
    // }

    // PG.loaded = 0;
    // function loadingCompleted() {
    //     PG.loaded++;
    //     var msg = Ext.get('loadmask').dom;
    //     if (PG.loaded === 1) {
    //         msg.innerHTML = msg.innerHTML + "<p>There's one. Go, Mr. Ferret!</p>";
    //     } else if (PG.loaded === 2) {
    //         msg.innerHTML = msg.innerHTML + "<p>There's another. (Pardon me, I have to get him some more coffee.)</p>";
    //     } else if (PG.loaded === 3) {
    //         msg.innerHTML = msg.innerHTML + "<p>That's done; fries should be up in two seconds. (Note to self: hire more ferrets.)</p>";
    //         createGrid();
    //     }
    // }

    PG.start = function() {
        Ext.History.init();
        // Handle this change event in order to restore the UI to the appropriate history state
        Ext.History.on('change', function(token){
            if (PG.style.transactionId) { Ext.Ajax.abort(PG.style.transactionId); }
            if (PG.topic.transactionId) { Ext.Ajax.abort(PG.topic.transactionId); }
            if (PG.combination.transactionId) { Ext.Ajax.abort(PG.combination.transactionId); }
            if (token) {
                selectBook(Ext.util.JSON.decode(token));
            } else {
                PG.text_searchbox.setValue('');
            }
        });
    }

    //     PG.loadMask = new Ext.LoadMask(Ext.getBody(), {
    //         msg: '<h3>This may take a while...</h3>' +
    //             '<p>The ferret is shovelling coal into the boiler as fast as he can.</p>' +
    //             '<p id="loadmask">(Ok, technically, we\'re loading the gigantic blobs of data so that I don\'t have to pay for server time.)</p>'
    //     });
    //     PG.loadMask.show();

    //     Ext.Ajax.request({
    //         url: "styledata.json",
    //         success: function(res, opt) {
    //             PG.styleStore = new Ext.data.Store({
    //                 storeId:     'styleStore',
    //                 reader:      PG.styledataReader,
    //                 data:        Ext.util.JSON.decode(res.responseText)
    //             });
    //             loadingCompleted();
    //         },
    //         failure: function(res, opt) {
    //             Ext.Msg.alert("Error loading style data", "Cannot load data about book styles.");
    //             console.log("styledata.json failure");
    //         },
    //         timeout: 60 * 1000,
    //     });

    //     Ext.Ajax.request({
    //         url: "topicdata.json",
    //         success: function(res, opt) {
    //             PG.topicStore = new Ext.data.Store({
    //                 autoLoad: false,
    //                 storeId:  'topicStore',
    //                 reader:   PG.topicdataReader,
    //                 data:     Ext.util.JSON.decode(res.responseText)
    //             });
    //             loadingCompleted();
    //         },
    //         failure: function(res, opt) {
    //             Ext.Msg.alert("Error loading topic data", "Cannot load data about book topics.");
    //             console.log("topiddata.json failure");
    //         },
    //         timeout: 60 * 1000,
    //     });

    //     Ext.Ajax.request({
    //         url: "metadata.json",
    //         success: function(res, opt) {
    //             PG.metadataStore = new Ext.data.Store({
    //                 storeId:     'metadataStore',
    //                 reader:      PG.metadataReader,
    //                 remoteSort:  false,
    //                 sortInfo:    { field:'author', direction:'ASC' },
    //                 data:        Ext.util.JSON.decode(res.responseText),
    //             });
    //             loadingCompleted();
    //         },
    //         failure: function(res, opt) {
    //             Ext.Msg.alert("Error loading metadata", "Cannot load general information about books.");
    //             console.log("metadata.json failure");
    //         },
    //         timeout: 60 * 1000,
    //     });
    // }
    
}());
