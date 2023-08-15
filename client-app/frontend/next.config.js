module.exports = () => {
  // const rewrites = () => {
    // return [
    //   {
    //     source: "/api",
    //     destination: "http://localhost:8080/api",
    //   },
    // ];
  // };
  return {
    // rewrites,
    reactStrictMode: true,
    output: 'export',
    distDir: '../target/classes/static',
    images: {
      unoptimized: true,
    }
  };
};