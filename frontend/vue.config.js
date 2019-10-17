module.exports = {
  assetsDir: 'static',

  devServer: {
    port: 9091,
    proxy: 'http://localhost:9393',
    disableHostCheck: true
  },

  configureWebpack: config => {
    config.performance = {
      // todo #88: enable back when we properly split into chunks
      hints: false
    }
  },

  chainWebpack: config => {
    // custom loader for CLDR data
    config.module
        .rule('cldr-data')
        .test(/\.cldr-data$/)
        .use('cldr-data')
        .loader(require('path').resolve('src/loaders/cldr-data-loader'))
        .end()

    // workaround for globalize with webpack
    config
        .resolve
        .alias
        .set('cldr$', 'cldrjs')
        .set('cldr', 'cldrjs/dist/cldr')
        .end()
  }
}