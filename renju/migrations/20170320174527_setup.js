
exports.up = function(knex, Promise) {
    return Promise.all([
        knex.schema.createTable('games', function(table) {
            table.increments('id');
            table.string('user1');
            table.string('user2');
            table.json('data');
            table.string('status');
            table.timestamps(true, true);
        })
    ])
};

exports.down = function(knex, Promise) {
    return Promise.all([
        knex.schema.dropTable('games')
    ])
};
