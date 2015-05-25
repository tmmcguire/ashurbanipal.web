(function() {
    "use strict";

    Ext.namespace('PG');

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
        Ext.History.add('B' + Ext.util.JSON.encode(selectedBook));
        selectedBook.distance = "";
        PG.bookTpl.overwrite(Ext.get('book-info'), selectedBook);
        PG.style.reset();
        startRequest(PG.style, selectedBook.etext_no);
        PG.topic.reset();
        startRequest(PG.topic, selectedBook.etext_no);
        PG.combination.reset();
        startRequest(PG.combination, selectedBook.etext_no);
    }

    function displayResults(query, response) {
        query.transactionId = undefined;
        query.rows = query.rows.concat( Ext.decode(response.responseText).rows );
        if (query.rows && query.rows.length) {
            for (var i = 0; i < 3; ++i) {
                var data = query.rows[query.current + i];
                data.distance = data.dist.toFixed(3) + ' ' + query.metric;
                var elt = Ext.get(query.eltBase + (i + 1));
                PG.bookTpl.overwrite(elt, data);
                elt.unmask();
            }
        } else {
            for (var i = 0; i < 3; ++i) {
                var elt = Ext.get(eltBase + (i+1));
                elt.dom.innerHTML = i === 1 ? 'No results available' : '';
                elt.mask();
            }
        }
        query.loadMask.hide();
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
        eltBase: 'style',
        metric: 'ell',
        transactionId: undefined,
        rows: [],
        current: 1,

        loadMask: new Ext.LoadMask(Ext.get('style-row'), {
            msg: '<h3>Loading...</h3>'
        }),

        reset: function() {
            PG.style.rows = [];
            PG.style.current = 1;
        },

        success: function(response, options) {
            displayResults(PG.style, response);
        },

        failure: function(response, options) {
            PG.style.transactionId = undefined;
        }
    };

    PG.topic = {
        url: 'data/topic',
        eltBase: 'topic',
        metric: 'bole',
        transactionId: undefined,
        rows: [],
        current: 1,

        loadMask: new Ext.LoadMask(Ext.get('topic-row'), {
            msg: '<h3>Loading...</h3>'
        }),

        reset: function() {
            PG.topic.rows = [];
            PG.topic.current = 1;
        },

        success: function(response, options) {
            displayResults(PG.topic, response);
        },

        failure: function(response, options) {
            PG.topic.transactionId = undefined;
        }

    };

    PG.combination = {
        url: 'data/combination',
        eltBase: 'combined',
        metric: 'ell bole',
        transactionId: undefined,
        rows: [],
        current: 1,

        loadMask: new Ext.LoadMask(Ext.get('combined-row'), {
            msg: '<h3>Loading...</h3>'
        }),

        reset: function() {
            PG.combination.rows = [];
            PG.combination.current = 1;
        },

        success: function(response, options) {
            displayResults(PG.combination, response);
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
                if (token.charAt(0) === 'B') {
                    selectBook(Ext.util.JSON.decode(token.substr(1)));
                }
            } else {
                PG.text_searchbox.setValue('');
            }
        });
        Ext.get('search').focus();
    }
    
}());
