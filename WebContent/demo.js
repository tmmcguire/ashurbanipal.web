(function() {
    "use strict";

    Ext.namespace('PG');

    function coalesce_conversion(name) { return function(v,rec) { return rec[name].join('; '); } }
    function pick_first_conversion(name) { return function(v,rec) { return rec[name][0]; } }

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

    function selectBook(selectedBook) {
        Ext.History.add(Ext.util.JSON.encode(selectedBook));
        PG.bookTpl.overwrite(Ext.get('book-info'), selectedBook);
        startRequest(PG.style, selectedBook.etext_no);
        startRequest(PG.topic, selectedBook.etext_no);
        startRequest(PG.combination, selectedBook.etext_no);
    }

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
                    var elt = Ext.get('style' + i);
                    elt.unmask();
                    PG.bookTpl.overwrite(elt, data);
                }
            } else {
                for (var i = 1; i < 4; ++i) {
                    var elt = Ext.get('style' + i);
                    elt.dom.innerHTML = '';
                    elt.mask();
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
                    var elt = Ext.get('topic' + i);
                    elt.unmask();
                    PG.bookTpl.overwrite(elt, data);
                }
            } else {
                for (var i = 1; i < 4; ++i) {
                    var elt = Ext.get('topic' + i);
                    elt.dom.innerHTML = i === 2 ? 'No results available' : '';
                    elt.mask();
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
                    var elt = Ext.get('combined' + i);
                    PG.bookTpl.overwrite(elt, data);
                    elt.unmask();
                }
            } else {
                for (var i = 1; i < 4; ++i) {
                    var elt = Ext.get('combined' + i);
                    elt.dom.innerHTML = i === 2 ? 'No results available' : '';
                    elt.mask();
                }
                
            }
            PG.combination.loadMask.hide();
        },

        failure: function(response, options) {
            PG.combination.transactionId = undefined;
        }

    };

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

    PG.text_searchbox = new Ext.form.ComboBox({
        store: PG.text_store,
        loadingText: 'Searching...',
        width: 570,
        pageSize: 20,
        hideTrigger: true,
        tpl: PG.text_tmpl,
        applyTo: 'search',
        itemSelector: 'div.search-item',
        listeners: {
            'select': function(combo, record, index) {
                if (!record) { return; }
                selectBook(record.data);
            },
        },
    });

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
    
}());
