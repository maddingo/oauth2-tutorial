import useSWR from 'swr';
import style from './quotes.module.css';

// Get quotes from remote API
// Read: https://medium.com/technest/next-js-oauth-with-nextauth-js-53a9f2b9994f
// Read: https://next-auth.js.org/getting-started/client
// Read: https://www.devgould.com/jwt-authentication-with-nextjs-bff-backend-for-frontend/

export default function Quotes({withRefreshButton = false, quoteUri: quoteUri = '/api/quote'}) {

  const fetcher = (url) => fetch(url).then((res) => res.json());
  const { data, mutate, error } = useSWR(quoteUri, fetcher);
  if (error) return <div className={style.content}><div className={style.error}>failed to load</div></div>;
  if (!data) return <div className={style.content}><div className={style.loading}>loading...</div></div>;
  return (
    <div className={style.content}>
      <h2 className={style.header}>{data.author} Quote</h2>
      <div className={style.area}>
        <p className={style.single}>{data.quote}</p>
      </div>
        {
          withRefreshButton && (
            <div className={style.buttonPanel}>
            <button className={style.button} onClick={() => mutate(data)}>Refresh</button>
            </div>
          )
        }
    </div>
  );
}
