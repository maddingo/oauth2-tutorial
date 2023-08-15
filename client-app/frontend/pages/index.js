import Layout from "../components/layout";
import Quotes from "../components/quotes";
import {useSession} from "next-auth/react";

export default function Home() {
  const {data: session} = useSession()

  return (

      <Layout>

        <Quotes quoteUri = {'/api/quote'} withRefreshButton = {false} />
        {session ? (
            // This URL needs login
            <Quotes quoteUri = {'/api/quote1'} withRefreshButton = {true} />
          ) : (
            <></>
          )
        }
      </Layout>
  )
}
